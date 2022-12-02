package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.vertx.common.VertxInstance;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * @author Bruno Salmon
 */
final class VertxHttpRouterFinaliser {

    static Router finaliseVertxHttpRouter() {
        Router router = VertxInstance.getHttpRouter();

        // GWT perfect caching (xxx.cache.xxx files will never change again)
        router.routeWithRegex(".*\\.cache\\..*").handler(routingContext -> {
            routingContext.response().putHeader("cache-control", "public, max-age=31556926");
            routingContext.next();
        });

        // SPA root page shouldn't be cached (to always return the latest version with the latest GWT compilation)
        router.routeWithRegex(".*/#/.*").handler(routingContext -> {
            routingContext.response().putHeader("cache-control", "public, max-age=0");
            routingContext.next();
        });

        // Serving static files under the webroot folder
        router.route("/*").handler(StaticHandler.create()); // Default one day MAX_AGE is ok except for root index page (how to fix that?)

        router.route("/*").handler(BodyHandler.create());

        return router;
    }

}
