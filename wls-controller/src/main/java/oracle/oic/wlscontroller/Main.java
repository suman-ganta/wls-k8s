package oracle.oic.wlscontroller;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class.
 *
 */
public class Main {
    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "http://0.0.0.0:8080/wls/";
    private static org.slf4j.Logger LOG = LoggerFactory.getLogger("oracle.oic.wlscontroller");

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
        ResourceConfig config = new ResourceConfig();
        config.registerClasses(WlsController.class);
        config.registerClasses(DebugController.class);
        config.register(JacksonFeature.class);

        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    /**
     * Main method.
     * @param args
     */
    public static void main(String[] args) {
        enableLoggers();
        final HttpServer server = startServer();
        LOG.info(String.format("wls-controller running at %s", BASE_URI));
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
    }

    private static void enableLoggers() {
        Logger l = Logger.getLogger("org.glassfish.grizzly.http.server.HttpHandler");
        l.setLevel(Level.FINE);
        l.setUseParentHandlers(false);
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);
        l.addHandler(ch);
    }
}

