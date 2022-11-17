package dev.webfx.stack.routing.router.spi.impl.vertx;

import dev.webfx.platform.async.Handler;
import dev.webfx.stack.routing.router.Route;
import dev.webfx.stack.routing.router.RoutingContext;

/**
 * @author Bruno Salmon
 */
final class VertxRoute implements Route {

    private final io.vertx.ext.web.Route vertxRoute;

    private VertxRoute(io.vertx.ext.web.Route vertxRoute) {
        this.vertxRoute = vertxRoute;
    }

    static VertxRoute create(io.vertx.ext.web.Route vertxRoute) {
        return new VertxRoute(vertxRoute);
    }

    @Override
    public Route path(String path) {
        return VertxRoute.create(vertxRoute.path(path));
    }

    @Override
    public Route pathRegex(String path) {
        return VertxRoute.create(vertxRoute.pathRegex(path));
    }

    @Override
    public String getPath() {
        return vertxRoute.getPath();
    }

    @Override
    public Route handler(Handler<RoutingContext> handler) {
        return VertxRoute.create(vertxRoute.handler(vrc -> handler.handle(VertxRoutingContext.create(vrc))));
    }

    @Override
    public Route failureHandler(Handler<RoutingContext> exceptionHandler) {
        return VertxRoute.create(vertxRoute.failureHandler(vrc -> exceptionHandler.handle(VertxRoutingContext.create(vrc))));
    }
}
