package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.boot.ApplicationReadiness;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.vertx.VertxInstance;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Bruno Salmon
 */
final class VertxHttpRouterConfigurator {

    // Set to true by VertxHttpVerticle once the HTTP server is listening and serving the
    // finalised router; reset on shutdown. Used by the /health endpoint below as the
    // load-balancer readiness signal (e.g. AWS ALB target group health check).
    static volatile boolean serverReady = false;

    static Router initialiseRouter() {
        Vertx vertx = VertxInstance.getVertx();
        Router router = Router.router(vertx);

        // Logging web requests
        router.route().handler(LoggerHandler.create());

        // Lightweight health/readiness endpoint for load balancers (e.g. AWS ALB target group
        // health checks). Returns 200 only once the HTTP server is fully started and serving the
        // finalised router AND all application readiness gates are completed (e.g. DB migrations —
        // see ApplicationReadiness); 503 while still booting, waiting on a gate, or draining.
        // Registered before the session and static handlers so it stays cheap, session-free, and
        // always takes precedence over the SPA routes (unlike /index.html, which is a static file
        // ready before the app and matched only for text/html requests). See VertxHttpVerticle,
        // which sets serverReady on listen success.
        router.route(HttpMethod.GET, "/health").handler(routingContext -> {
            boolean listening = serverReady;
            boolean ready = listening && ApplicationReadiness.areAllGatesReady();
            routingContext.response()
                .putHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0")
                .putHeader("Content-Type", "text/plain; charset=UTF-8")
                .setStatusCode(ready ? 200 : 503)
                .end(ready ? "OK" : !listening ? "STARTING" : "WAITING: " + ApplicationReadiness.pendingGateNames());
        });

        // The session store to use
        router.route().handler(SessionHandler.create(VertxInstance.getSessionStore()));

        // Redirect /frontend/* routes to the legacy system KBS2
        // TODO: remove this once KBS2 is decommissioned
        router.route("/frontend/*").handler(routingContext -> {
            String path = routingContext.request().path();
            String query = routingContext.request().query();
            String redirectUrl = "https://legacy.kadampabookings.org" + path + (query != null ? "?" + query : "");
            routingContext.response()
                .setStatusCode(302)
                .putHeader("Location", redirectUrl)
                .end();
        });

        // Transparent proxy for /dev/backend/* routes to the legacy system (used for initial KBS2 update just after
        // installation; this requires a true proxy rather than a redirect to follow the request chain correctly)
        // TODO: remove this once KBS2 is decommissioned
        HttpClient legacyProxyClient = vertx.createHttpClient(new HttpClientOptions()
            .setSsl(true)
            .setDefaultHost("legacy.kadampabookings.org")
            .setDefaultPort(443)
            .setConnectTimeout(10000)
            .setIdleTimeout(120));

        router.route("/dev/backend/*").handler(routingContext -> {
            var incomingRequest = routingContext.request();
            String path = incomingRequest.path();
            String query = incomingRequest.query();
            String targetUri = query != null ? path + "?" + query : path;
            Console.log("⇒ Proxying " + incomingRequest.method() + " " + path + " → https://legacy.kadampabookings.org" + path);
            RequestOptions requestOptions = new RequestOptions()
                .setHost("legacy.kadampabookings.org")
                .setPort(443)
                .setURI(targetUri)
                .setMethod(incomingRequest.method());
            // Forward all incoming request headers except Host (replaced by the target host)
            incomingRequest.headers().forEach(entry -> {
                if (!entry.getKey().equalsIgnoreCase("Host"))
                    requestOptions.putHeader(entry.getKey(), entry.getValue());
            });
            legacyProxyClient.request(requestOptions)
                .onFailure(cause -> routingContext.response()
                    .setStatusCode(502)
                    .end("Legacy proxy connection error: " + cause.getMessage()))
                .onSuccess(proxyRequest -> {
                    boolean hasBody = incomingRequest.method() != HttpMethod.GET && incomingRequest.method() != HttpMethod.HEAD;
                    (hasBody ? proxyRequest.send(incomingRequest) : proxyRequest.send())
                        .onFailure(cause -> routingContext.response()
                            .setStatusCode(502)
                            .end("Legacy proxy error: " + cause.getMessage()))
                        .onSuccess(proxyResponse -> {
                            routingContext.response().setStatusCode(proxyResponse.statusCode());
                            // Forward all response headers as-is
                            proxyResponse.headers().forEach(entry ->
                                routingContext.response().putHeader(entry.getKey(), entry.getValue()));
                            // Stream the response body directly
                            proxyResponse.pipeTo(routingContext.response())
                                .onFailure(cause -> {
                                    if (!routingContext.response().ended())
                                        routingContext.response().end();
                                });
                        });
                });
        });


        // SPA root page shouldn't be cached (to always return the latest version with the latest GWT compilation).
        // We assume the SPA is hosted under the root / or under any path ending with / or /index.html or any path
        // including /#/ (which is used for UI routing).
        router.routeWithRegex(".*").handler(routingContext -> {
            routingContext.response()
                .putHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0")
                .putHeader("Pragma", "no-cache")
                .putHeader("Expires", "0");
            routingContext.next();
        });

        // GWT perfect caching (xxx.cache.js files will never change again)
        router.routeWithRegex(".*\\.cache\\.js").handler(routingContext -> {
            routingContext.response()
                .putHeader("Cache-Control", "public, max-age=31556926")
                .putHeader("Pragma", "public")
                .putHeader("Expires", "1000000000000")
            ;
            routingContext.next();
        });

        // React/Vite perfect caching: files under /assets/ are content-hashed (e.g. index-Dwk1fx0A.js,
        // vendor-l0sNRNKZ.js), so they are immutable and can be cached forever. A new build produces new
        // file names, and index.html (kept no-store by the catch-all .* route above) always points at the
        // latest ones - so the browser reuses cached chunks across refreshes instead of re-downloading them.
        // This overrides the no-store header set by the .* route (putHeader replaces the previous value).
        router.routeWithRegex(".*/assets/.*").handler(routingContext -> {
            routingContext.response()
                .putHeader("Cache-Control", "public, max-age=31556926, immutable")
                .putHeader("Pragma", "public")
                .putHeader("Expires", "1000000000000")
            ;
            routingContext.next();
        });

        /*
        // For xxx.nocache.js GWT files, "no-cache" would work also in theory, but in practice it seems that now
        // browsers - or at least Chrome - are not checking those files if index.html hasn't changed! A shame because
        // most of the time, this is those files that change (on each new GWT compilation) and not index.html. So,
        // to force the browser to check those files, we use "no-store" (even if it is less optimized).
        router.routeWithRegex(".*\\.nocache\\.js").handler(routingContext -> {
            routingContext.response().putHeader("Cache-Control", "public, max-age=0, no-store, must-revalidate");
            routingContext.next();
        });*/

        return router;
    }

    static void addStaticRoute(String routePattern, ReadOnlyAstArray hostnamePatterns, String pathToStaticFolder) throws IOException {
        if (hostnamePatterns == null)
            addStaticRoute(routePattern, (String) null, pathToStaticFolder);
        else {
            for (int i = 0; i < hostnamePatterns.size(); i++)
                addStaticRoute(routePattern, hostnamePatterns.getString(i), pathToStaticFolder);
        }
    }

    static void addStaticRoute(String routePattern, String hostnamePattern, String pathToStaticFolder) throws IOException {
        Router router = VertxInstance.getHttpRouter();
        Route route = router.route(routePattern);
        if (hostnamePattern != null)
            route = route.virtualHost(hostnamePattern);
        Path staticPath = Paths.get(pathToStaticFolder);
        boolean absolute = staticPath.isAbsolute();
        int bangIndex = pathToStaticFolder.indexOf("!/");
        if (bangIndex != -1) {
            Path path = extractArchivedFolder(pathToStaticFolder);
            pathToStaticFolder = path.toAbsolutePath().toString();
            absolute = true;
        }
        String finalPathToStaticFolder = pathToStaticFolder;
        boolean finalAbsolute = absolute;

        // Custom handler that checks file existence first, with pass-through for missing files
        route.handler(routingContext -> {
            String requestPath = routingContext.request().path();
            // Remove the route pattern prefix to get the file path
            String filePath = requestPath;
            if (routePattern.endsWith("/*")) {
                String prefix = routePattern.substring(0, routePattern.length() - 2);
                if (requestPath.startsWith(prefix)) {
                    filePath = requestPath.substring(prefix.length());
                }
            }

            // Try to resolve the actual file
            Path resolvedPath = Paths.get(finalPathToStaticFolder, filePath);

            if (Files.exists(resolvedPath) && Files.isRegularFile(resolvedPath)) {
                // File exists, serve it via StaticHandler
                StaticHandler.create(finalAbsolute ? FileSystemAccess.ROOT : FileSystemAccess.RELATIVE, finalPathToStaticFolder)
                    .handle(routingContext);
            } else {
                // File doesn't exist - pass to the next handler (API routes registered later take priority)
                routingContext.next();
            }
        });

        // SPA fallback registered as a last-resort route: only serves index.html after all other routes
        // (including API routes like /payment/*) have had a chance to handle the request.
        Path indexPath = Paths.get(finalPathToStaticFolder, "index.html");
        if (Files.exists(indexPath)) {
            String finalIndexPath = indexPath.toAbsolutePath().toString();
            Route fallbackRoute = router.route(routePattern).last();
            if (hostnamePattern != null)
                fallbackRoute = fallbackRoute.virtualHost(hostnamePattern);
            fallbackRoute.handler(routingContext -> {
                String acceptHeader = routingContext.request().getHeader("Accept");
                if (acceptHeader != null && acceptHeader.contains("text/html")) {
                    routingContext.response()
                        .putHeader("Content-Type", "text/html; charset=UTF-8")
                        .sendFile(finalIndexPath);
                } else {
                    routingContext.next();
                }
            });
        }
    }

    private static final Map<String, Path> EXTRACTED_ARCHIVED_FOLDERS = new HashMap<>();

    private static Path extractArchivedFolder(String archiveWithInternalPath) throws IOException {
        Path alreadyExtractedPath = EXTRACTED_ARCHIVED_FOLDERS.get(archiveWithInternalPath);
        if (alreadyExtractedPath != null)
            return alreadyExtractedPath;

        int sep = archiveWithInternalPath.indexOf("!/");
        String archiveFile = archiveWithInternalPath.substring(0, sep);
        String internalPath = archiveWithInternalPath.substring(sep + 2);

        URI archiveUri = URI.create("jar:" + Path.of(archiveFile).toUri());
        Path extractedPath = Files.createTempDirectory("webfx-archive-extracted-");

        try (FileSystem fs = FileSystems.newFileSystem(archiveUri, Map.of())) {
            Path rootInArchive = fs.getPath("/" + internalPath);
            try (Stream<Path> walk = Files.walk(rootInArchive)) {
                walk.forEach(source -> {
                    try {
                        Path dest = extractedPath.resolve(rootInArchive.relativize(source).toString());
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(dest);
                        } else {
                            Files.copy(source, dest);
                        }
                        dest.toFile().deleteOnExit();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }

        extractedPath.toFile().deleteOnExit();

        EXTRACTED_ARCHIVED_FOLDERS.put(archiveWithInternalPath, extractedPath);

        return extractedPath;
    }

    static void finaliseRouter() {
        Router router = VertxInstance.getHttpRouter();
        router.route().handler(BodyHandler.create());
    }
}
