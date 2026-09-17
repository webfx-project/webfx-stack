package dev.webfx.stack.routing.router.spi.impl.vertx;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.routing.router.Route;
import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.spi.impl.vertx.VertxSession;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

/**
 * @author Bruno Salmon
 */
public final class VertxRoutingContext implements RoutingContext {

    private final io.vertx.ext.web.RoutingContext vertxRoutingContext;
    private AstObject params;

    private VertxRoutingContext(io.vertx.ext.web.RoutingContext vertxRoutingContext) {
        this.vertxRoutingContext = vertxRoutingContext;
    }

    public static VertxRoutingContext create(io.vertx.ext.web.RoutingContext vertxRoutingContext) {
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
        HttpServerResponse response = vertxRoutingContext.response();

        // null → end with no body (status code already set by caller via setStatus / fail).
        if (responseBody == null) {
            response.end();
            return;
        }

        // byte[] → raw binary.
        if (responseBody instanceof byte[]) {
            putContentTypeIfAbsent(response, "application/octet-stream");
            response.end(Buffer.buffer((byte[]) responseBody));
            return;
        }

        // AST object / array → JSON serialisation (uses the same AST layer
        // the rest of the platform speaks; works for both AstObject and
        // ReadOnlyAstObject because the former extends the latter).
        if (responseBody instanceof ReadOnlyAstObject) {
            putContentTypeIfAbsent(response, "application/json;charset=utf-8");
            response.end(AST.formatObject((ReadOnlyAstObject) responseBody, "json"));
            return;
        }
        if (responseBody instanceof ReadOnlyAstArray) {
            putContentTypeIfAbsent(response, "application/json;charset=utf-8");
            response.end(AST.formatArray((ReadOnlyAstArray) responseBody, "json"));
            return;
        }

        // Default: text/plain. Callers that need HTML must use
        // sendHtmlResponse() explicitly — historically the default here was
        // text/html, which silently misclassified JSON / log strings / etc.
        putContentTypeIfAbsent(response, "text/plain;charset=utf-8");
        response.end(responseBody.toString());
    }

    @Override
    public void sendHtmlResponse(Object responseBody) {
        // Always force text/html — HTML callers (OAuth callback pages, etc.)
        // typically don't pre-set Content-Type, and even if they did, asking
        // for HTML explicitly means they want HTML.
        vertxRoutingContext.response()
            .putHeader("content-type", "text/html;charset=utf-8")
            .end(responseBody == null ? "" : responseBody.toString());
    }

    /** Adds the given Content-Type only if the caller hasn't already set one. */
    private static void putContentTypeIfAbsent(HttpServerResponse response, String contentType) {
        if (response.headers().get("content-type") == null) {
            response.putHeader("content-type", contentType);
        }
    }
}
