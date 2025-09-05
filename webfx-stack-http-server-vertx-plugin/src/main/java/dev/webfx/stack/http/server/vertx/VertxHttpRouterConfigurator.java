package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.util.vertx.VertxInstance;
import io.vertx.core.Vertx;
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

    static Router initialiseRouter() {
        Vertx vertx = VertxInstance.getVertx();
        Router router = Router.router(vertx);

        // Logging web requests
        router.route().handler(LoggerHandler.create());

        // The session store to use
        router.route().handler(SessionHandler.create(VertxInstance.getSessionStore()));

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

        /*// GWT perfect caching (xxx.cache.js files will never change again)
        router.routeWithRegex(".*\\.cache\\.js").handler(routingContext -> {
            routingContext.response()
                .putHeader("Cache-Control", "public, max-age=31556926")
                .putHeader("Pragma", "public")
                .putHeader("Expires", "1000000000000")
            ;
            routingContext.next();
        });

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
        if (bangIndex != - 1) {
            Path path = extractArchivedFolder(pathToStaticFolder);
            pathToStaticFolder = path.toAbsolutePath().toString();
            absolute = true;
        }
        route.handler(StaticHandler.create(absolute ? FileSystemAccess.ROOT : FileSystemAccess.RELATIVE, pathToStaticFolder));
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
