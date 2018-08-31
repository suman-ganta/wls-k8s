package oracle.oic.wlscontroller.models2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Labels {

  @JsonProperty("app")
  private String app;

  @JsonProperty("tenant")
  private String tenant;

  @JsonProperty("app")
  public String getApp() {
    return app;
  }

  @JsonProperty("app")
  public void setApp(String app) {
    this.app = app;
  }

  @JsonProperty("tenant")
  public String getTenant() {
    return tenant;
  }

  @JsonProperty("tenant")
  public void setTenant(String tenant) {
    this.tenant = tenant;
  }

  @Override public String toString() {
    return String.format("\"%s\" : {\"%s\" : \"%s\", \"%s\" : \"%s\"}", "labels", "app", app, "tenant", tenant);
  }
}
