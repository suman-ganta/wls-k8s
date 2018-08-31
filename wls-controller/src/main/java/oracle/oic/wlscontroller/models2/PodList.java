package oracle.oic.wlscontroller.models2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown=true)
public class PodList {
  @JsonProperty("items")
  private List<Pod> items;

  @JsonProperty("items")
  public List<Pod> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<Pod> items) {
    this.items = items;
  }
}
