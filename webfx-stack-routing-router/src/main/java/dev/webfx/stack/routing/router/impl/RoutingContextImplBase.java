package dev.webfx.stack.routing.router.impl;

import dev.webfx.platform.console.Console;
import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.WritableJsonObject;
import dev.webfx.stack.routing.router.Route;
import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.routing.router.session.Session;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Bruno Salmon
 */
public abstract class RoutingContextImplBase implements RoutingContext {

    protected final String mountPoint;
    protected final String path;
    protected final Collection<RouteImpl> routes;
    protected Iterator<RouteImpl> iter;
    protected Route currentRoute;
    private WritableJsonObject params;
    private Session session;
    private Object userPrincipal;

    RoutingContextImplBase(String mountPoint, String path, Collection<RouteImpl> routes, Object state) {
        this.mountPoint = mountPoint;
        this.path = path;
        this.routes = routes;
        this.params = (WritableJsonObject) state; // Is merging state and params the right thing to do?
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
            RouteImpl route = iter.next();
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

    void unhandledFailure(int statusCode, Throwable failure, RouterImpl router) {
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
    public WritableJsonObject getParams() {
        if (params == null)
            params = Json.createObject();
        return params;
    }

    @Override
    public Session session() {
        return session;
    }

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public Object userPrincipal() {
        return userPrincipal;
    }

    @Override
    public void setUserPrincipal(Object userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    @Override
    public void clearUser() {
        setUserPrincipal(null);
    }

    public static RoutingContext newRedirectedContext(RoutingContext context, String redirectPath) {
        if (context instanceof RoutingContextImpl) {
            RoutingContextImpl ctx = (RoutingContextImpl) context;
            return new RoutingContextImpl(ctx.mountPoint(), ctx.router(), redirectPath, ctx.routes, ctx.getParams());
        }
        if (context instanceof SubRoutingContext) {
            SubRoutingContext ctx = (SubRoutingContext) context;
            return new SubRoutingContext(ctx.mountPoint(), redirectPath, ctx.routes, ctx.inner);
        }
        return null; // Shouldn't happen
    }

}
