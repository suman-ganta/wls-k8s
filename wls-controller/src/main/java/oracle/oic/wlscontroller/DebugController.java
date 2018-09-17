package oracle.oic.wlscontroller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("debug")
public class DebugController {
  private static Logger LOG = LoggerFactory.getLogger(DebugController.class);

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public String podNotification(String input) {
    LOG.info(input);
    return "{}";
  }
}
