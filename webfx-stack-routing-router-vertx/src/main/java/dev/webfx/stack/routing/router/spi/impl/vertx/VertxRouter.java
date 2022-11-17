package dev.webfx.stack.routing.router.spi.impl.vertx;

import dev.webfx.platform.async.Handler;
import dev.webfx.stack.routing.router.Route;
import dev.webfx.stack.routing.router.Router;
import dev.webfx.stack.routing.router.RoutingContext;

/**
 * @author Bruno Salmon
 */
final class VertxRouter implements Router {

    private final io.vertx.ext.web.Router vertxRouter;

    private VertxRouter(io.vertx.ext.web.Router vertxRouter) {
        this.vertxRouter = vertxRouter;
    }

    static VertxRouter create(io.vertx.ext.web.Router vertxRouter) {
        return new VertxRouter(vertxRouter);
    }

    @Override
    public Route route() {
        return VertxRoute.create(vertxRouter.route());
    }

    @Override
    public Route route(String path) {
        return VertxRoute.create(vertxRouter.route(path));
    }

    @Override
    public Route routeWithRegex(String path) {
        return VertxRoute.create(vertxRouter.routeWithRegex(path));
    }

    @Override
    public void accept(String path, Object state) {
    }

    @Override
    public Router mountSubRouter(String mountPoint, Router subRouter) {
        vertxRouter.mountSubRouter(mountPoint, ((VertxRouter) subRouter).vertxRouter);
        return this;
    }

    @Override
    public Router exceptionHandler(Handler<Throwable> exceptionHandler) {
        throw new UnsupportedOperationException(); // TODO: see what's the new way to do this in Vert.x
    }

    @Override
    public void handleContext(RoutingContext ctx) {
        vertxRouter.handleContext(((VertxRoutingContext) ctx).getVertxRoutingContext());
    }

    @Override
    public void handleFailure(RoutingContext ctx) {
        vertxRouter.handleFailure(((VertxRoutingContext) ctx).getVertxRoutingContext());
    }
}
