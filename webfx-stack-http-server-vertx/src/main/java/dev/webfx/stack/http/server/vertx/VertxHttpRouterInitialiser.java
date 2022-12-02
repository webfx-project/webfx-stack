package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.vertx.common.VertxInstance;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.SessionHandler;

/**
 * @author Bruno Salmon
 */
final class VertxHttpRouterInitialiser {

    static Router initialiseVertxHttpRouter() {
        Vertx vertx = VertxInstance.getVertx();
        Router router = Router.router(vertx);

        // Logging web requests
        router.route("/*").handler(LoggerHandler.create());

        router.route("/*").handler(SessionHandler.create(VertxInstance.getSessionStore()));

        return router;
    }

}
