package oracle.oic.wlscontroller.models2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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

  @Override public String toString() {
    return String.format("\"%s\" : {\"%s\" : %s, \"%s\" : %s}", "object", "metadata", metadata.toString(), "status", status.toString());
  }
}
