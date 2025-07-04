package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.vertx.common.VertxInstance;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * @author Bruno Salmon
 */
public class VertxHttpModuleBooter implements ApplicationModuleBooter {

    private final static String OPTIONS_CONFIG_PATH = "webfx.stack.http.options";
    private final static String ROUTES_CONFIG_PATH = "webfx.stack.http.routes";

    final static String HTTP_SERVERS_CONFIG_KEY = "httpServers";
    final static String PROTOCOL_CONFIG_KEY = "protocol";
    final static String PORT_CONFIG_KEY = "port";
    final static String CERT_PATH_CONFIG_KEY = "certPath";
    final static String KEY_PATH_CONFIG_KEY = "keyPath";

    static String HTTP_SERVER_PROTOCOL;
    static String HTTP_SERVER_PORT;
    static String HTTP_SERVER_ORIGIN;

    static ReadOnlyAstObject OPTIONS_CONFIGURATION;

    final static String HTTP_STATIC_ROUTES_CONFIG_KEY = "httpStaticRoutes";
    final static String ROUTE_PATTERN_CONFIG_KEY = "routePattern"; // obligatory
    final static String HOSTNAME_PATTERNS_CONFIG_KEY = "hostnamePatterns"; // optional
    final static String PATH_TO_STATIC_FOLDER_CONFIG_KEY = "pathToStaticFolder"; // obligatory

    @Override
    public String getModuleName() {
        return "webfx-stack-http-server-vertx";
    }

    @Override
    public int getBootLevel() {
        return COMMUNICATION_OPEN_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        // Using a local session store
        SessionStore sessionStore = LocalSessionStore.create(VertxInstance.getVertx());
        VertxInstance.setSessionStore(sessionStore);
        // Initialising the http router
        Router router = VertxHttpRouterConfigurator.initialiseRouter();
        VertxInstance.setHttpRouter(router);

        // 1) Starting HTTP servers
        ConfigLoader.onConfigLoaded(OPTIONS_CONFIG_PATH, config -> {

            if (config == null) {
                log("❌ No configuration found for http options (check " + OPTIONS_CONFIG_PATH + ")");
                return;
            }

            OPTIONS_CONFIGURATION = config;

            int errors = consumeEachValidHttpServerConfiguration(httpServerConfig -> {
                String protocol = httpServerConfig.getString(PROTOCOL_CONFIG_KEY);
                Console.log("🚀 Starting " + protocol + " server on port " + httpServerConfig.getString(PORT_CONFIG_KEY));
            }, true);

            //return errors == 0 ? Future.succeededFuture() : Future.failedFuture(new ConfigurationException(errors < CONFIGURATION.getArray(HTTP_SERVERS_CONFIG_KEY).size()));
        });

        // 2) Installing Vert.x bus (should be already set by VertxBusModuleBooter)
        Runnable bridgeInstaller = VertxInstance.getBridgeInstaller();
        if (bridgeInstaller != null)
            bridgeInstaller.run();
        else
            log("❌ Vert.x bridge installer was not set on time");

        // 3) HTTP routes
        ConfigLoader.onConfigLoaded(ROUTES_CONFIG_PATH, config -> {

            if (config == null) {
                log("❌ No configuration found for http routes (check " + ROUTES_CONFIG_PATH + ")");
                return;
            }

            int errors = consumeEachValidHttpStaticRouteConfiguration(config, httpStaticRouteConfig -> {
                String routePattern = httpStaticRouteConfig.getString(ROUTE_PATTERN_CONFIG_KEY);
                ReadOnlyAstArray hostnamePatterns = httpStaticRouteConfig.getArray(HOSTNAME_PATTERNS_CONFIG_KEY);
                String pathToStaticFolder = httpStaticRouteConfig.getString(PATH_TO_STATIC_FOLDER_CONFIG_KEY);
                try {
                    VertxHttpRouterConfigurator.addStaticRoute(routePattern, hostnamePatterns, pathToStaticFolder);
                    Console.log("✓ Routed '" + routePattern + "' to serve static files at " + pathToStaticFolder + (hostnamePatterns == null ? "" : " for the following hostnames: " + list(hostnamePatterns)));
                } catch (IOException e) {
                    Console.log("❌ Failed to route '" + routePattern + "' to serve static files at " + pathToStaticFolder + (hostnamePatterns == null ? "" : " for the following hostnames: " + list(hostnamePatterns)), e);
                }
            });

            //return errors == 0 ? Future.succeededFuture() : Future.failedFuture(new ConfigurationException(errors < configuration.getArray(HTTP_STATIC_ROUTES_CONFIG_KEY).size()));

        });
    }

    static int consumeEachValidHttpServerConfiguration(Consumer<ReadOnlyAstObject> consumer, boolean logInvalid) {
        int errors = 0;
        ReadOnlyAstArray httpServers = OPTIONS_CONFIGURATION.getArray(HTTP_SERVERS_CONFIG_KEY);
        for (int i = 0; i < httpServers.size(); i++) {
            ReadOnlyAstObject httpServerConfig = httpServers.getObject(i);
            if (checkHttpServerConfig(httpServerConfig, logInvalid))
                consumer.accept(httpServerConfig);
            else
                errors++;
        }
        return errors;
    }

    static boolean checkHttpServerConfig(ReadOnlyAstObject httpServerConfig, boolean logInvalid) {
        String protocol = httpServerConfig.getString(PROTOCOL_CONFIG_KEY);
        String port = httpServerConfig.getString(PORT_CONFIG_KEY);
        String certPath = httpServerConfig.getString(CERT_PATH_CONFIG_KEY);
        String keyPath = httpServerConfig.getString(KEY_PATH_CONFIG_KEY);
        if (areValuesNonNullAndResolved(protocol, port)
            && areValuesNullOrResolved(certPath, keyPath)
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
            Console.log("⛔️️ INVALID HTTP: Couldn't start http server due to invalid configuration: " + AST.formatObject(httpServerConfig, "json"));
        return false;
    }

    private static int consumeEachValidHttpStaticRouteConfiguration(ReadOnlyAstObject configuration, Consumer<ReadOnlyAstObject> consumer) {
        int errors = 0;
        ReadOnlyAstArray httpStaticRoutes = configuration.getArray(HTTP_STATIC_ROUTES_CONFIG_KEY);
        for (int i = 0; i < httpStaticRoutes.size(); i++) {
            ReadOnlyAstObject httpStaticRouteConfig = httpStaticRoutes.getObject(i);
            if (checkHttpStaticRouteConfig(httpStaticRouteConfig))
                consumer.accept(httpStaticRouteConfig);
            else
                errors++;
        }
        return errors;
    }

    private static boolean checkHttpStaticRouteConfig(ReadOnlyAstObject httpStaticRouteConfig) {
        String routePattern = httpStaticRouteConfig.getString(ROUTE_PATTERN_CONFIG_KEY);
        String pathToStaticFolder = httpStaticRouteConfig.getString(PATH_TO_STATIC_FOLDER_CONFIG_KEY);
        StaticFolder staticFolder = new StaticFolder(pathToStaticFolder);
        boolean mandatoryFieldsPresent = areValuesNonNullAndResolved(routePattern, pathToStaticFolder);
        boolean hostnamePatternsCorrect = !httpStaticRouteConfig.has(HOSTNAME_PATTERNS_CONFIG_KEY) || httpStaticRouteConfig.isArray(HOSTNAME_PATTERNS_CONFIG_KEY) && checkArrayOfResolvedStrings(httpStaticRouteConfig.getArray(HOSTNAME_PATTERNS_CONFIG_KEY));
        boolean staticFolderExists = staticFolder.exists();
        if (mandatoryFieldsPresent && hostnamePatternsCorrect && staticFolderExists) {
            // Reaching this code block indicates that the http configuration is valid.
            // Returning true to indicate this configuration is valid
            return true;
        }
        StringBuilder invalidMessage = new StringBuilder("⛔️️ INVALID ROUTE: Couldn't create static route due to invalid configuration: " + AST.formatObject(httpStaticRouteConfig, "json") + " because");
        if (!mandatoryFieldsPresent)
            invalidMessage.append(" " + ROUTE_PATTERN_CONFIG_KEY + " and/or " + PATH_TO_STATIC_FOLDER_CONFIG_KEY + " are absent or contains unresolved variables");
        if (!hostnamePatternsCorrect)
            invalidMessage.append(" " + HOSTNAME_PATTERNS_CONFIG_KEY + " = ").append(routePattern).append(" is invalid");
        if (!staticFolderExists) {
            invalidMessage.append(" " + PATH_TO_STATIC_FOLDER_CONFIG_KEY + " = ").append(pathToStaticFolder).append(" doesn't exist");
            File parentFolder = staticFolder.getParentFolder();
            File[] files = parentFolder == null ? null : parentFolder.listFiles();
            if (files != null) {
                invalidMessage.append(". Here is the content of the parent folder ").append(parentFolder.getAbsolutePath()).append(":");
                for (File file : files) {
                    invalidMessage.append("\n").append(file.getAbsolutePath());
                }
            }
        }
        Console.log(invalidMessage.toString());
        return false;
    }

    private static boolean checkArrayOfResolvedStrings(ReadOnlyAstArray hostnamePatterns) {
        for (int i = 0; i < hostnamePatterns.size(); i++) {
            if (!(hostnamePatterns.getElement(i) instanceof String))
                return false;
            if (!areValuesNonNullAndResolved(hostnamePatterns.getString(i)))
                return false;
        }
        return true;
    }

    private static String list(ReadOnlyAstArray hostnamePatterns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hostnamePatterns.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append('\'').append(hostnamePatterns.getString(i)).append('\'');
        }
        return sb.toString();
    }

    // Code copied from dev.webfx.stack.conf.ConfigurationService;
    // TODO: provides a similar feature in new substitution API
    public static boolean areValuesNullOrResolved(String... values) {
        for (String value : values)
            if (value != null && VARIABLE_PATTERN.matcher(value).find())
                return false;
        return true;
    }

    public static boolean areValuesNonNullAndResolved(String... values) {
        for (String value : values)
            if (value == null || VARIABLE_PATTERN.matcher(value).find())
                return false;
        return true;
    }

    public static Pattern VARIABLE_PATTERN = Pattern.compile("\\$?\\{\\{(.+)}}");

    private static class StaticFolder {

        private boolean expectingDirectory;
        private final File fileOrArchive;

        public StaticFolder(String pathToFolder) {
            if (pathToFolder == null)
                fileOrArchive = null;
            else {
                expectingDirectory = true;
                int exclamationIndex = pathToFolder.indexOf('!'); // means that the folder is actually inside a .zip file (or .war)
                if (exclamationIndex != -1) {
                    expectingDirectory = false;
                    pathToFolder = pathToFolder.substring(0, exclamationIndex);
                }
                fileOrArchive = Path.of(pathToFolder).toFile();
            }
        }

        public boolean exists() {
            return fileOrArchive != null && fileOrArchive.exists() && fileOrArchive.isDirectory() == expectingDirectory;
        }

        public File getParentFolder() {
            return fileOrArchive == null ? null : fileOrArchive.getParentFile();
        }
    }
}
