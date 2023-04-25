package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.keyobject.ReadOnlyIndexedArray;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.conf.ConfigurationService;
import dev.webfx.stack.conf.spi.impl.ConfigurationException;
import dev.webfx.stack.conf.spi.impl.resource.DefaultResourceConfigurationConsumer;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public class VertxHttpStaticRoutesConfigurationConsumer extends DefaultResourceConfigurationConsumer {

    private static final String CONFIGURATION_NAME = "HttpStaticRoutes";
    private final static String DEFAULT_CONFIGURATION_RESOURCE_FILE_NAME = "HttpStaticRoutes.default.json";
    final static String HTTP_STATIC_ROUTES_CONFIG_KEY = "httpStaticRoutes";
    final static String ROUTE_PATTERN_CONFIG_KEY = "routePattern"; // obligatory
    final static String HOSTNAME_PATTERNS_CONFIG_KEY = "hostnamePatterns"; // optional
    final static String PATH_TO_STATIC_FOLDER_CONFIG_KEY = "pathToStaticFolder"; // obligatory


    public VertxHttpStaticRoutesConfigurationConsumer() {
        super(CONFIGURATION_NAME, DEFAULT_CONFIGURATION_RESOURCE_FILE_NAME);
    }

    @Override
    protected Future<Void> boot(ReadOnlyKeyObject configuration) {
        int errors = consumeEachValidHttpStaticRouteConfiguration(configuration, httpStaticRouteConfig -> {
            String routePattern = httpStaticRouteConfig.getString(ROUTE_PATTERN_CONFIG_KEY);
            ReadOnlyIndexedArray hostnamePatterns = httpStaticRouteConfig.getArray(HOSTNAME_PATTERNS_CONFIG_KEY);
            String pathToStaticFolder = httpStaticRouteConfig.getString(PATH_TO_STATIC_FOLDER_CONFIG_KEY);
            Console.log("Routing '" + routePattern + "' to serve static files at " + pathToStaticFolder + (hostnamePatterns == null ? "" : " for the following hostnames: " + list(hostnamePatterns)));
            VertxHttpRouterConfigurator.addStaticRoute(routePattern, hostnamePatterns, pathToStaticFolder);
        });

        return errors == 0 ? Future.succeededFuture() : Future.failedFuture(new ConfigurationException(errors < configuration.getArray(HTTP_STATIC_ROUTES_CONFIG_KEY).size()));
    }

    private static int consumeEachValidHttpStaticRouteConfiguration(ReadOnlyKeyObject configuration, Consumer<ReadOnlyKeyObject> consumer) {
        int errors = 0;
        ReadOnlyIndexedArray httpStaticRoutes = configuration.getArray(HTTP_STATIC_ROUTES_CONFIG_KEY);
        for (int i = 0; i < httpStaticRoutes.size(); i++) {
            ReadOnlyKeyObject httpStaticRouteConfig = httpStaticRoutes.getObject(i);
            if (checkHttpStaticRouteConfig(httpStaticRouteConfig))
                consumer.accept(httpStaticRouteConfig);
            else
                errors++;
        }
        return errors;
    }

    private static boolean checkHttpStaticRouteConfig(ReadOnlyKeyObject httpStaticRouteConfig) {
        String routePattern = httpStaticRouteConfig.getString(ROUTE_PATTERN_CONFIG_KEY);
        String pathToStaticFolder = httpStaticRouteConfig.getString(PATH_TO_STATIC_FOLDER_CONFIG_KEY);
        if (ConfigurationService.areValuesNonNullAndResolved(routePattern, pathToStaticFolder)
                && (!httpStaticRouteConfig.has(HOSTNAME_PATTERNS_CONFIG_KEY) || httpStaticRouteConfig.isArray(HOSTNAME_PATTERNS_CONFIG_KEY) && checkArrayOfResolvedStrings(httpStaticRouteConfig.getArray(HOSTNAME_PATTERNS_CONFIG_KEY)))
                && checkFolderExists(pathToStaticFolder) ) {
            // Reaching this code block indicates that the http configuration is valid.
            // Returning true to indicate this configuration is valid
            return true;
        }
        Console.log("⚠️ WARNING: Invalid static route configuration: " + httpStaticRouteConfig);
        return false;
    }

    private static boolean checkFolderExists(String pathToFolder) {
        boolean expectingDirectory = true;
        int exclamationIndex = pathToFolder.indexOf('!'); // means that the folder is actually inside a .zip file (or .war)
        if (exclamationIndex != -1) {
            expectingDirectory = false;
            pathToFolder = pathToFolder.substring(0, exclamationIndex);
        }
        File file = Path.of(pathToFolder).toFile();
        return file.exists() && file.isDirectory() == expectingDirectory;
    }

    private static boolean checkArrayOfResolvedStrings(ReadOnlyIndexedArray hostnamePatterns) {
        for (int i = 0; i < hostnamePatterns.size(); i++) {
            if (!(hostnamePatterns.getElement(i) instanceof String))
                return false;
            if (!ConfigurationService.areValuesNonNullAndResolved(hostnamePatterns.getString(i)))
                return false;
        }
        return true;
    }

    private static String list(ReadOnlyIndexedArray hostnamePatterns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hostnamePatterns.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append('\'').append(hostnamePatterns.getString(i)).append('\'');
        }
        return sb.toString();
    }

}
