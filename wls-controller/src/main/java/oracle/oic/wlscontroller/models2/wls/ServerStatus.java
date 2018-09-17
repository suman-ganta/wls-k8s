package oracle.oic.wlscontroller.models2.wls;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ServerStatus {
  @JsonProperty("name")
  private String name;
  @JsonProperty("state")
  private String state;

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("state")
  public String getState() {
    return state;
  }

  @JsonProperty("state")
  public void setState(String state) {
    this.state = state;
  }

  @Override public String toString() {
    return String.format("{\"name\" : \"%s\", \"state\" : \"%s\"}", name, state);
  }
}
