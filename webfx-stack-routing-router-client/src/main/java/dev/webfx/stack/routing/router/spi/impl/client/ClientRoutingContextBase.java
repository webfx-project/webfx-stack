package dev.webfx.stack.routing.router.spi.impl.client;

import dev.webfx.platform.console.Console;
import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.stack.routing.router.Route;
import dev.webfx.stack.routing.router.RoutingContext;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Bruno Salmon
 */
public abstract class ClientRoutingContextBase implements RoutingContext {

    protected final String mountPoint;
    protected final String path;
    protected final Collection<ClientRoute> routes;
    protected Iterator<ClientRoute> iter;
    protected Route currentRoute;
    private AstObject params;

    ClientRoutingContextBase(String mountPoint, String path, Collection<ClientRoute> routes, Object state) {
        this.mountPoint = mountPoint;
        this.path = path;
        this.routes = routes;
        this.params = (AstObject) state; // Is merging state and params the right thing to do?
        iter = routes.iterator();
    }

    @Override
    public String mountPoint() {
        return mountPoint;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Route currentRoute() {
        return currentRoute;
    }

    @Override
    public void next() {
        iterateNext();
    }

    boolean iterateNext() {
        boolean failed = failed();
        while (iter.hasNext()) {
            ClientRoute route = iter.next();
            if (route.matches(this, mountPoint(), failed)) {
                //Logger.log("Route matches: " + route);
                try {
                    currentRoute = route;
                    //Logger.log("Calling the " + (failed ? "failure" : "") + " handler");
                    if (failed)
                        route.handleFailure(this);
                    else
                        route.handleContext(this);
                } catch (Throwable t) {
                    Console.log("Throwable thrown from handler", t);
                    if (!failed) {
                        Console.log("Failing the routing");
                        fail(t);
                    } else {
                        // Failure in handling failure!
                        Console.log("Failure in handling failure");
                        unhandledFailure(-1, t, route.router());
                    }
                } finally {
                    currentRoute = null;
                }
                return true;
            }
        }
        return false;
    }

    void unhandledFailure(int statusCode, Throwable failure, ClientRouter router) {
        //int code = statusCode != -1 ? statusCode : 500;
        if (failure != null) {
            if (router.exceptionHandler() != null)
                router.exceptionHandler().handle(failure);
            else
                Console.log("Unexpected exception in route", failure);
        }
        /*if (!response().ended()) {
            try {
                response().setStatusCode(code);
            } catch (IllegalArgumentException e) {
                // means that there are invalid chars in the status message
                response()
                        .setStatusMessage(HttpResponseStatus.valueOf(code).reasonPhrase())
                        .setStatusCode(code);
            }
            response().end(response().getStatusMessage());
        }*/
    }

    @Override
    public AstObject getParams() {
        if (params == null)
            params = AST.createObject();
        return params;
    }

    public static RoutingContext newRedirectedContext(RoutingContext context, String redirectPath) {
        if (context instanceof ClientRoutingContextImpl) {
            ClientRoutingContextImpl ctx = (ClientRoutingContextImpl) context;
            return new ClientRoutingContextImpl(ctx.mountPoint(), ctx.router(), redirectPath, ctx.routes, ctx.getParams());
        }
        if (context instanceof SubClientRoutingContext) {
            SubClientRoutingContext ctx = (SubClientRoutingContext) context;
            return new SubClientRoutingContext(ctx.mountPoint(), redirectPath, ctx.routes, ctx.inner);
        }
        return null; // Shouldn't happen
    }

}
