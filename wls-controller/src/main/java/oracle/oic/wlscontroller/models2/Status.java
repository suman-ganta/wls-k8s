package oracle.oic.wlscontroller.models2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Status {
  @JsonProperty("podIP")
  private String podIP;

  @JsonProperty("phase")
  private String phase;

  @JsonProperty("podIP")
  public String getPodIP() {
    return podIP;
  }

  @JsonProperty("podIP")
  public void setPodIP(String podIP) {
    this.podIP = podIP;
  }

  @JsonProperty("phase")
  public String getPhase() {
    return phase;
  }

  @JsonProperty("phase")
  public void setPhase(String phase) {
    this.phase = phase;
  }

  @Override public String toString() {
    return String.format("{\"%s\" : %s, \"%s\", \"%s\"}", "podIP", podIP, "phase", phase);
  }
}
