package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.keyobject.ReadOnlyIndexedArray;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.platform.vertx.common.VertxInstance;
import dev.webfx.stack.conf.ConfigurationService;
import dev.webfx.stack.conf.spi.impl.ConfigurationException;
import dev.webfx.stack.conf.spi.impl.resource.DefaultResourceConfigurationConsumer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public final class VertxHttpConfigurationConsumer extends DefaultResourceConfigurationConsumer {

    private static final String CONFIGURATION_NAME = "HttpOptions";
    final static String HTTP_SERVERS_CONFIG_KEY = "httpServers";
    final static String PROTOCOL_CONFIG_KEY = "protocol";
    final static String PORT_CONFIG_KEY = "port";
    final static String CERT_PATH_CONFIG_KEY = "certPath";
    final static String KEY_PATH_CONFIG_KEY = "keyPath";

    static String HTTP_SERVER_PROTOCOL;
    static String HTTP_SERVER_PORT;
    static String HTTP_SERVER_ORIGIN;

    static ReadOnlyKeyObject CONFIGURATION;

    public VertxHttpConfigurationConsumer() {
        super(CONFIGURATION_NAME, "HttpOptions.default.json");
        // Using a local session store
        SessionStore sessionStore = LocalSessionStore.create(VertxInstance.getVertx());
        VertxInstance.setSessionStore(sessionStore);
        // Initialising the http router
        Router router = VertxHttpRouterInitialiser.initialiseVertxHttpRouter();
        VertxInstance.setHttpRouter(router);
    }

    static boolean checkHttpServerConfig(ReadOnlyKeyObject httpServerConfig, boolean logInvalid) {
        String protocol = httpServerConfig.getString(PROTOCOL_CONFIG_KEY);
        String port = httpServerConfig.getString(PORT_CONFIG_KEY);
        String certPath = httpServerConfig.getString(CERT_PATH_CONFIG_KEY);
        String keyPath = httpServerConfig.getString(KEY_PATH_CONFIG_KEY);
        if (ConfigurationService.areValuesNonNullAndResolved(protocol, port)
                && ConfigurationService.areValuesNullOrResolved(certPath, keyPath)
                && (certPath == null && keyPath == null || certPath != null && keyPath != null && Files.exists(Path.of(certPath)) && Files.exists(Path.of(keyPath)))) {
            // Reaching this code block indicates that the http configuration is valid.
            // We set the HTTP_SERVER_XXX global variables from the first valid http configuration:
            if (HTTP_SERVER_PROTOCOL == null) {
                HTTP_SERVER_PROTOCOL = protocol;
                HTTP_SERVER_PORT = port;
                boolean isUsingDefaultPort = "http".equals(protocol) && "80".equals(port) || "https".equals(protocol) && "443".equals(port);
                HTTP_SERVER_ORIGIN = protocol + "://${{ SERVER_HOST }}" + (isUsingDefaultPort ? "" : ":" + port);
            }
            // Returning true to indicate this configuration is valid
            return true;
        }
        if (logInvalid)
            Console.log("⚠️ WARNING: Couldn't start " + protocol + " server on port " + port + " because the configuration is invalid");
        return false;
    }

    static int consumeEachValidHttpServerConfiguration(Consumer<ReadOnlyKeyObject> consumer, boolean logInvalid) {
        int errors = 0;
        ReadOnlyIndexedArray httpServers = CONFIGURATION.getArray(HTTP_SERVERS_CONFIG_KEY);
        for (int i = 0; i < httpServers.size(); i++) {
            ReadOnlyKeyObject httpServerConfig = httpServers.getObject(i);
            if (checkHttpServerConfig(httpServerConfig, logInvalid))
                consumer.accept(httpServerConfig);
            else
                errors++;
        }
        return errors;
    }

    @Override
    protected Future<Void> boot(ReadOnlyKeyObject config) {
        CONFIGURATION = config;

        int errors = consumeEachValidHttpServerConfiguration(httpServerConfig -> {
            String protocol = httpServerConfig.getString(PROTOCOL_CONFIG_KEY);
            Console.log("Starting " + protocol + " server on port " + httpServerConfig.getString(PORT_CONFIG_KEY));
        }, true);

        return errors == 0 ? Future.succeededFuture() : Future.failedFuture(new ConfigurationException(errors < CONFIGURATION.getArray(HTTP_SERVERS_CONFIG_KEY).size()));
    }
}
