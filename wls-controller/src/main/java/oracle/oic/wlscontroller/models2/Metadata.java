package oracle.oic.wlscontroller.models2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Metadata {
  @JsonProperty("name")
  private String name;

  @JsonProperty("namespace")
  private String namespace;

  @JsonProperty("labels")
  private Labels labels;

  @JsonProperty("labels")
  public Labels getLabels() {
    return labels;
  }

  @JsonProperty("labels")
  public void setLabels(Labels labels) {
    this.labels = labels;
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("namespace")
  public String getNamespace() {
    return namespace;
  }

  @JsonProperty("namespace")
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  @Override public String toString() {
    return String.format("{%s}", labels.toString());
  }
}
