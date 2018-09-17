package oracle.oic.wlscontroller;

import oracle.oic.wlscontroller.models2.Labels;
import oracle.oic.wlscontroller.models2.Parent;
import oracle.oic.wlscontroller.models2.Pod;
import oracle.oic.wlscontroller.models2.PodList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Scanner;

/**
 * Root resource (exposed at "controller" path)
 */
@Path("controller")
public class WlsController {

  private static Logger LOG = LoggerFactory.getLogger("oracle.oic.wlscontroller");
  private final String WLS_ADMIN = "wls-admin";
  private final String WLS_MS = "wls-ms";
  private final String TOKEN_LOC = "/var/run/secrets/kubernetes.io/serviceaccount/token";

  @GET @Produces(MediaType.APPLICATION_JSON)
  public String ping() {
    return "{\"controller\" : \"WlsController\"}";
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response podNotification(Parent parent) {
    //LOG.info("Parent: " + parent);
    final Pod pod = parent.getPod();
    if(pod == null) return null;

    //check if pod is ready
    if (!"Running".equals(pod.getStatus().getPhase())) {
      return null;
    }
    final Labels labels = pod.getMetadata().getLabels();
    final String app = labels.getApp();

    if (WLS_ADMIN.equals(app)) {
      LOG.info("Admin Server: " + pod.getMetadata().getName());
      registerOrphanServers(pod);
    } else {
      LOG.info("Managed Server: " + pod.getMetadata().getName());
      registerToAdmin(pod);
    }
    return Response.ok(pod).build();
  }

  private void registerOrphanServers(Pod adminPod) {
    String tenant = adminPod.getMetadata().getLabels().getTenant();
    //check other admin servers for this tenant are not active.
    final List<Pod> adminPods = findTenantPods(adminPod, WLS_ADMIN);
    if(adminPods == null){
      //should not happen as adminPod should be return at least.
      return;
    }
    for (Pod pod : adminPods) {
      if (!pod.getMetadata().getName().equals(adminPod.getMetadata().getName())) {
        LOG.error(
            String.format("There is an active admin server '%s' running "
                + "for the tenant '%s'. Ignoring this (%s) server",
                pod.getMetadata().getName(), tenant, adminPod.getMetadata().getName()));
      }
    }
    //now lookup and register managed servers
    final List<Pod> managedPods = findTenantPods(adminPod, WLS_MS);
    if(managedPods != null) {
      final String adminPodIP = adminPod.getStatus().getPodIP();
      ensureAdminServer();
      for (Pod managedPod : managedPods) {
        registerManagedPod(adminPodIP, managedPod);
      }
    }
  }

  //FIXME
  private void ensureAdminServer() {
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void registerToAdmin(Pod msPod) {
    String adminServer = getAdminServerFor(msPod);
    LOG.info(String.format("Admin server for '%s' is '%s'", msPod.getMetadata().getName(), adminServer));
    if (adminServer == null) {
      return;
    }

    try {
      registerManagedPod(adminServer, msPod);
    } catch (RuntimeException e) {
      LOG.error(e.getMessage());
    }
  }

  private void registerManagedPod(String adminServer, Pod managedPod) {
    LOG.info(String.format("Registering '%s' to '%s'", managedPod.getMetadata().getName(), adminServer));
    WlsClient wlsClient = new WlsClient(adminServer, managedPod);

    try {
      //1. start edit session
      wlsClient.startEdit();
      //2. create machine
      wlsClient.createMachineIfNotExists();
      //3. create server
      wlsClient.createServerIfNotExists();
      //4. activate changes.
      wlsClient.activate();
      LOG.info("starting server..");
      wlsClient.startServer();
      LOG.info("STATUS: " + wlsClient.getServers());
    } catch (RuntimeException e) {
      LOG.error(e.getMessage(), e);
      wlsClient.cancelEdit();
      throw e;
    }
  }

  private String getAdminServerFor(Pod managedPod) {
    String tenant = managedPod.getMetadata().getLabels().getTenant();
    final List<Pod> pods = findTenantPods(managedPod, WLS_ADMIN);
    if (pods != null) {
      LOG.info("Admin PODs" + pods);
      if(pods.size() > 1){
        LOG.error("Found more than one admin server pods for the tenant: " + tenant);
      } else if (pods.size() == 0) {
        LOG.error("No admin server pod for the tenant: " + tenant);
        return null;
      }

      Pod p = pods.get(0);
      return p.getStatus().getPodIP();
    }
    return null;
  }

  private List<Pod> findTenantPods(Pod pod, String label){
    String tenant = pod.getMetadata().getLabels().getTenant();
    String namespace = pod.getMetadata().getNamespace();

    String apiServer = System.getenv("KUBERNETES_SERVICE_HOST");
    if (apiServer == null) {
      return null;
    }
    String port = System.getenv("KUBERNETES_PORT_443_TCP_PORT");
    String baseUri = String.format("https://%s:%s/api/v1/namespaces/%s/pods/", apiServer, port, namespace);
    LOG.info("API Server: " + baseUri);
    final Client client = getClient();
    PodList podList = client.target(baseUri)
        .queryParam("labelSelector", "tenant%3D" + tenant + ",app%3D" + label)
        .request().get(PodList.class);
    final List<Pod> pods = podList.getItems();
    return pods;
  }

  private Client getClient(){
    try {

      SSLContext sslcontext = SSLContext.getInstance("TLS");

      sslcontext.init(null, new TrustManager[] {
          new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
            }

            public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
            }

            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }
          }
      }, new java.security.SecureRandom());

      Client c = ClientBuilder.newBuilder()
          .sslContext(sslcontext)
          .hostnameVerifier((s1, s2) -> true)
          .build();

      String token = new Scanner(Paths.get(TOKEN_LOC)).next();
      if(token != null) {
        c.register((ClientRequestFilter) requestContext -> requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token));
      }
      return c;

    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
    return null;
  }
}
