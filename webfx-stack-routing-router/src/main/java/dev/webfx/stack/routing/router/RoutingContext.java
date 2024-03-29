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

    default void sendResponse(Object responseBody) {}

    /*

    String normalisedPath();

    Cookie getCookie(String name);

    RoutingContext addCookie(Cookie cookie);

    Cookie removeCookie(String name);

    int cookieCount();

    Set<Cookie> cookies();

    */

}
