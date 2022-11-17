package dev.webfx.stack.routing.router.spi.impl.client;

import dev.webfx.platform.async.Handler;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.routing.router.Route;
import dev.webfx.stack.routing.router.Router;
import dev.webfx.stack.routing.router.RoutingContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class ClientRouter implements Router {

    private final List<ClientRoute> routes = new ArrayList<>();
    private Handler<Throwable> exceptionHandler;

    @Override
    public Route route() {
        return new ClientRoute(this);
    }

    @Override
    public Route route(String path) {
        return route().path(path);
    }

    @Override
    public Route routeWithRegex(String path) {
        return route().pathRegex(path);
    }

    void addRoute(ClientRoute route) {
        routes.add(route);
    }

    @Override
    public void accept(String path, Object state) {
        Console.log("Routing " + path);
        new ClientClientRoutingContext(null, this, path, routes, state).next();
    }

    @Override
    public synchronized Router exceptionHandler(Handler<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    Handler<Throwable> exceptionHandler() {
        return exceptionHandler;
    }

    @Override
    public Router mountSubRouter(String mountPoint, Router subRouter) {
        if (mountPoint.endsWith("*"))
            throw new IllegalArgumentException("Don't include * when mounting subrouter");
        if (mountPoint.contains(":"))
            throw new IllegalArgumentException("Can't use patterns in subrouter mounts");
        route(mountPoint + "*").handler(subRouter::handleContext).failureHandler(subRouter::handleFailure);
        return this;
    }

    @Override
    public void handleContext(RoutingContext ctx) {
        new SubClientRoutingContext(getAndCheckRoutePath(ctx), ctx.path(), routes, ctx).next();
    }

    @Override
    public void handleFailure(RoutingContext ctx) {
        new SubClientRoutingContext(getAndCheckRoutePath(ctx), ctx.path(), routes, ctx).next();
    }

    private String getAndCheckRoutePath(RoutingContext ctx) {
        Route currentRoute = ctx.currentRoute();
        String path = currentRoute.getPath();
        if (path == null)
            throw new IllegalStateException("Sub routers must be mounted on constant paths (no regex or patterns)");
        return path;
    }

    Iterator<ClientRoute> iterator() {
        return routes.iterator();
    }
}
