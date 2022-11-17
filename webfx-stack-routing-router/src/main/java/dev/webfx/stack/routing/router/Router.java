package dev.webfx.stack.routing.router;

import dev.webfx.platform.async.Handler;

/**
 * @author Bruno Salmon
 */
public interface Router {

    static Router create() { return RouterFactory.createRouter(); }

    Route route();

    Route route(String path);

    Route routeWithRegex(String path);

    void accept(String path, Object state);

    Router mountSubRouter(String mountPoint, Router subRouter);

    Router exceptionHandler(Handler<Throwable> exceptionHandler);

    void handleContext(RoutingContext ctx);

    void handleFailure(RoutingContext ctx);

}
