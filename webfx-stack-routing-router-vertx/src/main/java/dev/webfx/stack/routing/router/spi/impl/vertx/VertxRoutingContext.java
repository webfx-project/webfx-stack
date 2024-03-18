package dev.webfx.stack.routing.router.spi.impl.vertx;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.stack.routing.router.Route;
import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.spi.impl.vertx.VertxSession;

/**
 * @author Bruno Salmon
 */
public final class VertxRoutingContext implements RoutingContext {

    private final io.vertx.ext.web.RoutingContext vertxRoutingContext;
    private AstObject params;

    private VertxRoutingContext(io.vertx.ext.web.RoutingContext vertxRoutingContext) {
        this.vertxRoutingContext = vertxRoutingContext;
    }

    static VertxRoutingContext create(io.vertx.ext.web.RoutingContext vertxRoutingContext) {
        return new VertxRoutingContext(vertxRoutingContext);
    }

    public io.vertx.ext.web.RoutingContext getVertxRoutingContext() {
        return vertxRoutingContext;
    }

    @Override
    public String path() {
        return vertxRoutingContext.currentRoute().getPath();
    }

    @Override
    public void next() {
        vertxRoutingContext.next();
    }

    @Override
    public AstObject getParams() {
        if (params == null) {
            params = AST.createObject();
            vertxRoutingContext.request().params().forEach((name, value) -> params.set(name, value));
        }
        return params;
    }

    @Override
    public void fail(int statusCode) {
        vertxRoutingContext.fail(statusCode);
    }

    @Override
    public void fail(Throwable throwable) {
        vertxRoutingContext.fail(throwable);
    }

    @Override
    public String mountPoint() {
        return vertxRoutingContext.mountPoint();
    }

    @Override
    public Route currentRoute() {
        return VertxRoute.create(vertxRoutingContext.currentRoute());
    }

    @Override
    public int statusCode() {
        return vertxRoutingContext.statusCode();
    }

    @Override
    public boolean failed() {
        return vertxRoutingContext.failed();
    }

    @Override
    public Throwable failure() {
        return vertxRoutingContext.failure();
    }

    @Override
    public Session session() {
        return VertxSession.create(vertxRoutingContext.session());
    }

    @Override
    public void sendResponse(Object responseBody) {
        vertxRoutingContext.response().putHeader("content-type", "text/html").end(responseBody.toString());
    }
}
