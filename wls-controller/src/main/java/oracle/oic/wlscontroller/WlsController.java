package oracle.oic.wlscontroller;

import oracle.oic.wlscontroller.models2.*;
import oracle.oic.wlscontroller.models2.wls.ServerStatus;
import oracle.oic.wlscontroller.models2.wls.ServersStatus;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Root resource (exposed at "controller" path)
 */
@Path("controller")
public class WlsController {

  private static Logger LOG = LoggerFactory.getLogger("wlscontroller");
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
    final Response response = Response.ok("{\"labels\":{}}").build();
    final Pod pod = parent.getPod();
    if(pod == null) return null;

    //check if pod is ready
    if (!"Running".equals(pod.getStatus().getPhase())) {
      return response;
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
    //no new labels or annotations
    return response;
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
                + "for tenant '%s'. Ignoring this (%s) server",
                pod.getMetadata().getName(), tenant, adminPod.getMetadata().getName()));
      }
    }
    //now lookup and register managed servers
    final List<Pod> managedPods = findTenantPods(adminPod, WLS_MS);
    if(managedPods != null) {
      final String adminPodIP = adminPod.getStatus().getPodIP();
      ensureAdminServer();

      //clean-up dead managed pods registered to this admin server
      WlsClient wlsClient = new WlsClient(adminPod.getStatus().getPodIP(), null);
      final ServersStatus registeredServers = wlsClient.getServers();
      Map<String, Pod> currentManagedPods = new HashMap<>();
      for (Pod managedPod : managedPods) {
        currentManagedPods.put(managedPod.getMetadata().getName(), managedPod);
      }
      for (ServerStatus item : registeredServers.getItems()) {
        if (!item.getName().equals("AdminServer") && !currentManagedPods.containsKey(item.getName())) {
          //this pod is gone, remove it.
          LOG.info("Removing unavailable managed pod " + item.getName());
          Pod deletedPod = new Pod();
          final Metadata metadata = new Metadata();
          metadata.setName(item.getName());
          deletedPod.setMetadata(metadata);
          wlsClient.setPod(deletedPod);
          wlsClient.deleteMachineIfNotExists();
          wlsClient.deleteServerIfNotExists();
        }
      }

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
      if(wlsClient.createMachineIfNotExists()) {
        //3. create server
        wlsClient.createServerIfNotExists();
        //4. activate changes.
        wlsClient.activate();
        LOG.info("starting server..");
        wlsClient.startServer();
      }
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
