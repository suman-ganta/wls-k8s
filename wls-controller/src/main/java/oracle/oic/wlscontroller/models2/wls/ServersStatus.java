package oracle.oic.wlscontroller.models2.wls;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ServersStatus {
  @JsonProperty("items")
  private List<ServerStatus> items;

  @JsonProperty("items")
  public List<ServerStatus> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<ServerStatus> items) {
    this.items = items;
  }

  @Override public String toString() {
    return String.format("{ \"items\" : [%s]}", items.stream().map(Object::toString)
        .collect(Collectors.joining(", ")));
  }
}
