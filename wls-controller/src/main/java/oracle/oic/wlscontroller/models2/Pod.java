package oracle.oic.wlscontroller.models2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.ConnectException;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Pod {
  @JsonProperty("metadata")
  private Metadata metadata;

  @JsonProperty("status")
  private Status status;

  @JsonProperty("metadata")
  public Metadata getMetadata() {
    return metadata;
  }

  @JsonProperty("metadata")
  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  @JsonProperty("status")
  public Status getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(Status status) {
    this.status = status;
  }

  public boolean isRunning(){
    return status != null && "Running".equals(status.getPhase());
  }

  public boolean ping(){
    String baseUri = "http://" + getStatus().getPodIP() + ":7001/management/weblogic/latest/";
    Client c = ClientBuilder.newClient();
    try {
      c.target(baseUri).request().get();
    } catch (ProcessingException e) {
      if (e.getCause() instanceof ConnectException) {
        return false;
      }
    }
    return true;
  }

  @Override public String toString() {
    return String.format("\"%s\" : {\"%s\" : %s, \"%s\" : %s}", "object", "metadata", metadata.toString(), "status", status.toString());
  }
}
