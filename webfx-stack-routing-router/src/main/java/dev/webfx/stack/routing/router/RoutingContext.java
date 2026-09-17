package dev.webfx.stack.routing.router;

import dev.webfx.stack.session.Session;
import dev.webfx.platform.ast.AstObject;

/**
 * @author Bruno Salmon
 */
public interface RoutingContext {

    String path();

    void next();

    AstObject getParams();

    void fail(int statusCode);

    void fail(Throwable throwable);

    String mountPoint();

    Route currentRoute();

    int statusCode();

    boolean failed();

    Throwable failure();

    Session session();

    /**
     * Sends the response body to the client. The Content-Type is inferred from
     * the body's runtime type:
     * <ul>
     *   <li>{@code null} → empty body (current status code is preserved)</li>
     *   <li>{@code byte[]} → {@code application/octet-stream}</li>
     *   <li>{@link dev.webfx.platform.ast.ReadOnlyAstObject} /
     *       {@link dev.webfx.platform.ast.ReadOnlyAstArray} → {@code application/json}
     *       (serialised via {@link dev.webfx.platform.ast.AST#formatObject})</li>
     *   <li>{@code String} (or anything else) → {@code text/plain;charset=utf-8}</li>
     * </ul>
     * Callers that have already set an explicit {@code Content-Type} header
     * on the response have their choice respected — the inference is only a
     * default for callers that didn't.
     * <p>
     * For HTML responses, use {@link #sendHtmlResponse(Object)} instead of
     * relying on a content type default, since HTML is not the most common
     * case and being explicit avoids surprises.
     */
    default void sendResponse(Object responseBody) {}

    /**
     * Sends an HTML response. Equivalent to {@link #sendResponse(Object)} but
     * sets {@code Content-Type: text/html;charset=utf-8} regardless of the
     * body type. The body is converted to a string via {@code toString()};
     * pass a pre-rendered HTML string for the common case.
     * <p>
     * Used by login gateways (Facebook, Google, MojoAuth) for OAuth redirect
     * landing pages whose body is HTML that runs JavaScript in the user's
     * browser.
     */
    default void sendHtmlResponse(Object responseBody) {}

    /*

    String normalisedPath();

    Cookie getCookie(String name);

    RoutingContext addCookie(Cookie cookie);

    Cookie removeCookie(String name);

    int cookieCount();

    Set<Cookie> cookies();

    */

}
