package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.boot.ApplicationReadiness;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.substitution.Substitutor;
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
                if (!wantsThePage(routingContext.request().path(), routingContext.request().getHeader("Accept"))) {
                    routingContext.next();
                    return;
                }
                routingContext.response()
                    .putHeader("Content-Type", "text/html; charset=UTF-8")
                    .sendFile(finalIndexPath);
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

    /** The members the passkey-endpoints document may carry, in the order the spec lists them. */
    private static final String[] PASSKEY_ENDPOINT_MEMBERS = { "enroll", "manage", "prfUsageDetails" };

    /**
     * The passkey-endpoints document for this configuration, or null when there is nothing to say.
     *
     * <p>W3C "A Well-Known URL for Relying Party Passkey Endpoints": a JSON object whose optional
     * {@code enroll}, {@code manage} and {@code prfUsageDetails} members are URLs to the pages where a
     * person creates and manages their passkeys. Password managers fetch it so they can offer "manage
     * your passkeys for this site" as a link rather than leaving the user to hunt through settings.
     *
     * <p><b>An unresolved value is dropped, not served.</b> These URLs come from configuration and are
     * typically written as a substitution; if the variable behind one is unset, the config carries the
     * placeholder text itself. Publishing {@code "enroll": "${{ SOMETHING }}/profile"} would be worse
     * than publishing nothing — a manager would send people to a URL that cannot exist.
     *
     * <p><b>An empty document and no document are different answers, and the difference is the config
     * key.</b> The spec: "An empty JSON object CAN be returned to signal support for passkeys, but not
     * advertise specific endpoints." So {@code {}} says "passkeys work here, there is no page to send
     * you to", while a 404 says nothing at all. The key being ABSENT returns null and leaves the path
     * 404ing; the key being PRESENT always yields a document, {@code {}} if no member survives. That
     * is what KBS publishes as of 2026-10-23: passkeys are enrolled and managed in the back office
     * only, deliberately, so there is no page a member's password manager could usefully open — but
     * the credential they already hold does work here, and {@code {}} is how you say exactly that.
     * It also means a misconfiguration degrades to {@code {}} rather than to a bad URL; the boot log
     * prints the body, so an unexpected {@code {}} is visible there.
     *
     * <p>Built by hand rather than through an AST writer so that this plugin needs no JSON dependency it
     * does not already have. Three known keys and URL values, each escaped.
     */
    static String passkeyEndpointsBody(ReadOnlyAstObject config) {
        if (config == null)
            return null;
        StringBuilder body = new StringBuilder();
        for (String member : PASSKEY_ENDPOINT_MEMBERS) {
            String url = config.getString(member);
            if (url == null || url.isBlank() || !Substitutor.areValuesNonNullAndResolved(url))
                continue;
            body.append(body.length() == 0 ? "{" : ",")
                .append("\"").append(member).append("\":\"").append(escapeJson(url)).append("\"");
        }
        return body.length() == 0 ? "{}" : body.append("}").toString();
    }

    /** Minimal JSON string escaping — these are URLs, but a configuration file can hold anything. */
    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (c < 0x20)
                        escaped.append(String.format("\\u%04x", (int) c));
                    else
                        escaped.append(c);
            }
        }
        return escaped.toString();
    }

    /**
     * Serves the passkey-endpoints document at its well-known path.
     *
     * <p>200 and {@code application/json}, and NO redirect — the spec requires all three, so this is an
     * exact route rather than anything the SPA fallback could answer. The fallback would 404 it anyway:
     * the path's first segment starts with a dot. A route registered here matches first.
     */
    static void addPasskeyEndpointsRoute(String body) {
        VertxInstance.getHttpRouter()
            .route(HttpMethod.GET, "/.well-known/passkey-endpoints")
            .handler(routingContext -> routingContext.response()
                .putHeader("Content-Type", "application/json")
                .end(body));
    }

    /** Our own namespaces: a request here is an endpoint call, and a typo in one must fail as one. */
    private static final String[] API_PREFIXES = { "/rest/", "/eventbus/", "/payment/" };

    /**
     * Whether this request should be answered with the application shell.
     *
     * <p>Three reasons to say no, and the third one is the one that changed.
     *
     * <p>A path that names a FILE the static handler did not find is not a route (see
     * {@link #looksLikeMissingFile}). A path under one of our own API namespaces is an endpoint call:
     * answering HTML with a 200 for a typo'd endpoint is how a caller ends up parsing "&lt;" as JSON,
     * so those keep failing as endpoints.
     *
     * <p>Otherwise the shell is served unless the client asked for something that is explicitly NOT a
     * page. This used to require {@code Accept} to contain {@code text/html}, which browsers send and
     * almost nothing else does — so every link-preview fetch got a 404, and a front-office URL shared
     * on WhatsApp, Facebook or Slack rendered with no preview card at all. Measured on production,
     * 2026-10-22: 33 requests a day to /video-streaming alone, one of them literally
     * {@code WhatsApp/2.23.20.0}, the rest in-app Android fetches. An absent, blank or {@code &#42;/&#42;}
     * Accept now gets the page, which is what those clients send; {@code Accept: application/json}
     * still gets a 404.
     */
    static boolean wantsThePage(String path, String acceptHeader) {
        if (looksLikeMissingFile(path))
            return false;
        if (path != null)
            for (String prefix : API_PREFIXES)
                if (path.startsWith(prefix))
                    return false;
        if (acceptHeader == null || acceptHeader.isBlank())
            return true;
        return acceptHeader.contains("text/html") || acceptHeader.contains("*/*");
    }

    /**
     * Whether a path names a FILE rather than an application route.
     *
     * <p>Only reached once the static handler has looked and found nothing, so a true here means a file
     * that does not exist. Two things follow from answering 404 instead of the app shell. Internet
     * scanners asking for {@code /forum.php}, {@code /.env} or {@code /wp-login.php} stop being served a
     * 38 KB page each — they were, because they send {@code Accept: text/html} like any browser. And a
     * stale asset request after a deploy — {@code /assets/index-OLDHASH.js} — gets a 404 instead of HTML,
     * which is the difference between a clean failure and "Unexpected token '&lt;'" in the console.
     *
     * <p><b>A SHORT extension, which is the whole subtlety.</b> Signed tokens are dot-separated
     * ({@code base64(payload).expiry.base64(hmac)}), so {@code /magic-link/<token>} ends in a dot
     * followed by a 43-character base64 MAC. Treating any dot as a file extension would have made every
     * magic link, every invitation and every e-mail-change link 404 — sign-in by e-mail, gone. One to
     * five characters admits .php, .env, .git, .sql, .ini, .bak, .woff2 and the rest of the scanner
     * vocabulary while excluding anything a MAC can be.
     *
     * <p>Deliberately NOT a list of bad extensions: that is endless, always one pattern behind, and would
     * still have served the shell for {@code /backup.sql}.
     */
    static boolean looksLikeMissingFile(String path) {
        if (path == null)
            return false;
        // A segment beginning with a dot is a dotfile or a dot-directory — /.env, /.git/config,
        // /.aws/credentials — never a route here, and the first thing a credential scanner asks for.
        // Checked separately because /.git/config has no extension on its last segment at all.
        for (int i = 0; i < path.length() - 1; i++)
            if (path.charAt(i) == '/' && path.charAt(i + 1) == '.')
                return true;
        int lastSlash = path.lastIndexOf('/');
        String segment = lastSlash < 0 ? path : path.substring(lastSlash + 1);
        int dot = segment.lastIndexOf('.');
        if (dot < 0 || dot == segment.length() - 1)
            return false;
        String extension = segment.substring(dot + 1);
        if (extension.length() > 5)
            return false;
        for (int i = 0; i < extension.length(); i++)
            if (!Character.isLetterOrDigit(extension.charAt(i)))
                return false;
        return true;
    }

    /**
     * Whether a request failed because its BODY could not be decoded — a malformed or oversized
     * multipart form, which is what scanners produce when they POST junk at a route that takes uploads.
     *
     * <p>Matched on the class NAME so that nothing here has to import Netty's codec internals (and so
     * that the pom this plugin ships with needs no new dependency). The alternative is what we had: every
     * such probe logging "Unhandled exception in router" at ERROR, which is noise in the one signal an
     * operator reads to find real failures — and, in production, noise an alarm filter can trip over.
     */
    static boolean isBodyDecoderFailure(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause())
            if (t.getClass().getName().startsWith("io.netty.handler.codec"))
                return true;
        return false;
    }

    static void finaliseRouter() {
        Router router = VertxInstance.getHttpRouter();
        router.route().handler(BodyHandler.create());
        // A body that will not decode is the client's doing, not ours: answer 400 and keep it out of the
        // error log. Registered after the BodyHandler so it is in the chain for the routes that read a
        // body; anything else it does not recognise is passed on to the default handler untouched.
        router.route().failureHandler(routingContext -> {
            if (isBodyDecoderFailure(routingContext.failure())) {
                if (!routingContext.response().ended())
                    routingContext.response().setStatusCode(400).end();
                return;
            }
            routingContext.next();
        });
    }
}
