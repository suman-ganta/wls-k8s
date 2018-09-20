package oracle.oic.wlscontroller;

import oracle.oic.wlscontroller.models2.Pod;
import oracle.oic.wlscontroller.models2.wls.ServersStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.concurrent.locks.ReentrantLock;

public class WlsClient {
  private final String baseUri;
  private Pod pod;
  private final String adminServer;
  private Client c;
  private static Logger LOG = LoggerFactory.getLogger("wlscontroller");
  final String REQUESTED_BY = "X-Requested-By";
  private static ReentrantLock lock = new ReentrantLock();

  public WlsClient(String adminServer, Pod pod){
    this.adminServer = adminServer;
    this.baseUri = "http://" + adminServer + ":7001/management/weblogic/latest/";
    this.pod = pod;

    c = ClientBuilder.newClient();
    String token = Base64.getEncoder().encodeToString("weblogic:welcome1".getBytes());
    c.register((ClientRequestFilter) requestContext -> requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Basic " + token));
  }

  public String host(){
    return System.getenv("HOSTNAME");
  }

  public void setPod(Pod p){
    this.pod = p;
  }

  public void startEdit(){
      lock.lock();
      Response response = c.target(baseUri).path("edit/changeManager/startEdit").request().header(REQUESTED_BY, host()).post(Entity.json(null));
      if (response.getStatus() != 200) {
        LOG.error("response: " + response.getStatus());
        LOG.error("body: " + response.readEntity(String.class));
        throw new RuntimeException("startEdit failed");
      }
  }

  public void cancelEdit(){
    try {
      Response response = c.target(baseUri).path("edit/changeManager/cancelEdit").request().header(REQUESTED_BY, host()).post(Entity.json(null));
      if (response.getStatus() != 200) {
        LOG.error("response: " + response.getStatus());
        LOG.error("body: " + response.readEntity(String.class));
        throw new RuntimeException("cancel edit call failed");
      }
    }finally {
      if(lock.isLocked())
        lock.unlock();
    }
  }

  public void activate(){
    try {
      Response response = c.target(baseUri).path("edit/changeManager/activate").request().header(REQUESTED_BY, host()).post(Entity.json(null));
      if (response.getStatus() != 200) {
        LOG.error("response: " + response.getStatus());
        LOG.error("body: " + response.readEntity(String.class));
        throw new RuntimeException("Activation failed");
      }
    }finally {
      if(lock.isLocked())
        lock.unlock();
    }
  }

  public void startServer(){
    ensureNodeManager();
    Response response = c.target(baseUri).path("domainRuntime/serverLifeCycleRuntimes").path(pod.getMetadata().getName()).path("start")
        .request().header(REQUESTED_BY, host())
        .header("Prefer", "respond-async")
        .post(Entity.json("{}"));
    if (response.getStatus() != 202) {
      LOG.info("response: " + response.getStatus());
      LOG.trace("body: " + response.readEntity(String.class));
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

  public boolean createServerIfNotExists(){
    String serverName = pod.getMetadata().getName();
    String serverIp = pod.getStatus().getPodIP();
    String machineName = "machine_" + pod.getMetadata().getName();

    //1. check if server exists
    Response response = c.target(baseUri).path("edit/servers").path(serverName)
        .request().header(REQUESTED_BY, host())
        .get();
    if (response.getStatus() != 404) {
      LOG.trace("Server: " + serverName + " already exists");
      return false;
    }

    //2. create server
    response = c.target(baseUri).path("edit/servers")
        .request().header(REQUESTED_BY, host())
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
        .request().header(REQUESTED_BY, host())
        .post(Entity.entity(String.format("{\"machine\" : [ \"machines\", \"%s\" ], \"cluster\" : [ \"clusters\", \"%s\" ], \"listenAddress\" : \"%s\", "
            + "\"listenPort\" : \"%s\", \"externalDNSName\" : \"%s\", \"listenPortEnabled\" : true, \"arguments\" : \"%s\"}", machineName, "DockerCluster", serverIp, "7001", serverIp, args), MediaType.APPLICATION_JSON));

    if (response.getStatus() != 200) {
      final String resp = response.readEntity(String.class);
      LOG.error("Failed to set properties: " + serverName);
      LOG.trace(resp);
      throw new RuntimeException("Failed to set properties: " + serverName);
    }
    return true;
  }

  public void deleteServerIfExists(){
    String serverName = pod.getMetadata().getName();

    //1. check if server exists
    Response response = c.target(baseUri).path("edit/servers").path(serverName)
        .request().header(REQUESTED_BY, host())
        .get();
    if (response.getStatus() != 404) {
      response = c.target(baseUri).path("edit/servers").path(serverName)
          .request().header(REQUESTED_BY, host())
          .delete();
      LOG.trace("Delete server: " + response.getStatus() + " - " + response.readEntity(String.class));
    }
  }

  public void deleteMachineIfExists(){
    String machineName = "machine_" + pod.getMetadata().getName();

    //1. check if machine exists
    Response response = c.target(baseUri).path("edit/machines").path(machineName)
        .request().header(REQUESTED_BY, host())
        .get();
    if (response.getStatus() != 404) {
      //remove machine association first
      response = c.target(baseUri).path("edit/servers").path(pod.getMetadata().getName())
          .request().header(REQUESTED_BY, host())
          .post(Entity.entity("{\"machine\" : [ \"machines\", \"\" ], \"cluster\" : [ \"clusters\", \"\" ]}", MediaType.APPLICATION_JSON));
      LOG.trace("Remove association with machine: " + response.getStatus() + " - " + response.readEntity(String.class));

      response = c.target(baseUri).path("edit/machines").path(machineName)
          .request().header(REQUESTED_BY, host())
          .delete();
      LOG.trace("Delete machine: " + response.getStatus() + " - " + response.readEntity(String.class));
    }
  }


  public boolean createMachineIfNotExists(){
    String machineName = "machine_" + pod.getMetadata().getName();

    //1. check if machine exists
    Response response = c.target(baseUri).path("edit/machines").path(machineName)
        .request().header(REQUESTED_BY, host())
        .get();
    if (response.getStatus() != 404) {
      LOG.trace("Machine: " + machineName + " already exists");
      return false;
    }

    //2. create machine
    response = c.target(baseUri).path("edit/machines")
        .request().header(REQUESTED_BY, host())
        .post(Entity.entity(String.format("{\"name\" : \"%s\"}", machineName), MediaType.APPLICATION_JSON));
    if (response.getStatus() != 201) {
      LOG.error(String.format("code: %d, response: %s", response.getStatus(), response.readEntity(String.class)));
      throw new RuntimeException("Failed to create machine: " + machineName);
    }

    // 3. Set node manager properties
    response = c.target(baseUri).path("edit/machines").path(machineName).path("nodeManager")
        .request().header(REQUESTED_BY, host())
        .post(Entity.entity(String.format("{\"NMType\" : \"plain\", \"listenAddress\" : \"%s\"}", pod.getStatus().getPodIP()), MediaType.APPLICATION_JSON));
    if (response.getStatus() != 200) {
      LOG.error("Failed to set node manager listen address: " + machineName);
      throw new RuntimeException("Failed to set node manager listen address: " + machineName);
    }
    return true;
  }

  public ServersStatus getServers(){
    Response response = c.target(baseUri).path("domainRuntime/serverLifeCycleRuntimes")
        .queryParam("links", "none")
        .queryParam("fields", "name,state")
        .request().get();
    return response.readEntity(ServersStatus.class);
  }
}
