package oracle.oic.wlscontroller;

import oracle.oic.wlscontroller.models2.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.Base64;

public class WlsClient {
  private final String baseUri;
  private final Pod pod;
  private final String adminServer;
  private Client c;
  private static Logger LOG = LoggerFactory.getLogger("oracle.oic.wlscontroller");
  final String REQUESTED_BY = "X-Requested-By";

  public WlsClient(String adminServer, Pod pod){
    this.adminServer = adminServer;
    this.baseUri = "http://" + adminServer + ":7001/management/weblogic/latest/";
    this.pod = pod;

    c = ClientBuilder.newClient();
    String token = Base64.getEncoder().encodeToString("weblogic:welcome1".getBytes());
    c.register((ClientRequestFilter) requestContext -> requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Basic " + token));
  }

  public void startEdit(){
    Response response = c.target(baseUri).path("edit/changeManager/startEdit")
        .request()
        .header(REQUESTED_BY, pod.getMetadata().getName())
        .post(Entity.json(null));
    if (response.getStatus() != 200) {
      LOG.error("Start edit failed");
      throw new RuntimeException("startEdit failed");
    }
  }

  public void cancelEdit(){
    Response response = c.target(baseUri).path("edit/changeManager/cancelEdit")
        .request()
        .header(REQUESTED_BY, pod.getMetadata().getName())
        .post(Entity.json(null));
    if (response.getStatus() != 200) {
      throw new RuntimeException("cancel edit call failed");
    }
  }

  public void activate(){
    Response response = c.target(baseUri).path("edit/changeManager/activate")
        .request().header(REQUESTED_BY, pod.getMetadata().getName())
        .post(Entity.json(null));
    if (response.getStatus() != 200) {
      throw new RuntimeException("Activation failed");
    }
  }

  public void startServer(){
    ensureNodeManager();
    Response response = c.target(baseUri).path("domainRuntime/serverLifeCycleRuntimes").path(pod.getMetadata().getName()).path("start")
        .request().header(REQUESTED_BY, pod.getMetadata().getName())
        .header("Prefer", "respond-async")
        .post(Entity.json("{}"));
    if (response.getStatus() != 202) {
      LOG.info("response: " + response.getStatus());
      LOG.info("body: " + response.readEntity(String.class));
      throw new RuntimeException("Startup request failed");
    }
  }

  //FIXME
  private void ensureNodeManager() {
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void createServerIfNotExists(){
    String serverName = pod.getMetadata().getName();
    String machineName = "machine_" + pod.getMetadata().getName();

    //1. check if server exists
    Response response = c.target(baseUri).path("edit/servers").path(serverName)
        .request().header(REQUESTED_BY, pod.getMetadata().getName())
        .get();
    if (response.getStatus() != 404) {
      LOG.info("Server: " + serverName + " already exists");
      return;
    }

    //2. create server
    response = c.target(baseUri).path("edit/servers")
        .request().header(REQUESTED_BY, pod.getMetadata().getName())
        .post(Entity.entity(String.format("{\"name\" : \"%s\"}", serverName), MediaType.APPLICATION_JSON));

    if (response.getStatus() != 201) {
      LOG.error(String.format("code: %d, response: %s", response.getStatus(), response.readEntity(String.class)));
      LOG.error("Failed to create server: " + serverName);
      throw new RuntimeException("Failed to create server: " + serverName);
    }

    // 3. Set properties
    String args = String.format("-Djava.security.egd=file:/dev/./urandom -Dweblogic.Name=%s "
        + "-Dweblogic.management.server=http://%s:%s", serverName, adminServer, "7001");
    response = c.target(baseUri).path("edit/servers").path(serverName)
        .request().header(REQUESTED_BY, pod.getMetadata().getName())
        .post(Entity.entity(String.format("{\"machine\" : [ \"machines\", \"%s\" ], \"cluster\" : [ \"clusters\", \"%s\" ], \"listenAddress\" : \"\", "
            + "\"listenPort\" : \"%s\", \"externalDNSName\" : \"%s\", \"listenPortEnabled\" : true, \"arguments\" : \"%s\"}", machineName, "DockerCluster", "8001", serverName, args), MediaType.APPLICATION_JSON));

    if (response.getStatus() != 200) {
      final String resp = response.readEntity(String.class);
      LOG.error("Failed to set properties: " + serverName);
      LOG.info(resp);
      throw new RuntimeException("Failed to set properties: " + serverName);
    }
  }

  public String showServer(){
    LOG.info("Server details");
    Response response = c.target(baseUri).path("edit/servers").path(pod.getMetadata().getName()).request().get();
    return response.readEntity(String.class);
  }

  public void createMachineIfNotExists(){
    String machineName = "machine_" + pod.getMetadata().getName();

    //1. check if machine exists
    Response response = c.target(baseUri).path("edit/machines").path(machineName)
        .request().header(REQUESTED_BY, pod.getMetadata().getName())
        .get();
    if (response.getStatus() != 404) {
      LOG.info("Machine: " + machineName + " already exists");
      return;
    }

    //2. create machine
    response = c.target(baseUri).path("edit/machines")
        .request().header(REQUESTED_BY, pod.getMetadata().getName())
        .post(Entity.entity(String.format("{\"name\" : \"%s\"}", machineName), MediaType.APPLICATION_JSON));
    if (response.getStatus() != 201) {
      LOG.error(String.format("code: %d, response: %s", response.getStatus(), response.readEntity(String.class)));
      LOG.error("Failed to create machine: " + machineName);
      throw new RuntimeException("Failed to create machine: " + machineName);
    }

    // 3. Set node manager properties
    response = c.target(baseUri).path("edit/machines").path(machineName).path("nodeManager")
        .request().header(REQUESTED_BY, pod.getMetadata().getName())
        .post(Entity.entity(String.format("{\"NMType\" : \"plain\", \"listenAddress\" : \"%s\"}", pod.getStatus().getPodIP()), MediaType.APPLICATION_JSON));
    if (response.getStatus() != 200) {
      LOG.error("Failed to set node manager listen address: " + machineName);
      throw new RuntimeException("Failed to set node manager listen address: " + machineName);
    }
  }

  public String getServers(){
    Response response = c.target(baseUri).path("domainRuntime/serverLifeCycleRuntimes")
        .queryParam("links", "none")
        .queryParam("fields", "name,state")
        .request().get();
    return response.readEntity(String.class);
  }
}
