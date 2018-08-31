package oracle.oic.wlscontroller.models2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Parent {
  @JsonProperty("object")
  private Pod pod;

  @JsonProperty("object")
  public Pod getPod() {
    return pod;
  }

  @JsonProperty("object")
  public void setPod(Pod pod) {
    this.pod = pod;
  }

  @Override public String toString() {
    return String.format("{%s}", pod.toString());
  }
}
