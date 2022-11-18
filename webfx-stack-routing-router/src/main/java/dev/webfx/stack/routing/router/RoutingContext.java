package dev.webfx.stack.routing.router;

import dev.webfx.stack.session.Session;
import dev.webfx.platform.json.JsonObject;

/**
 * @author Bruno Salmon
 */
public interface RoutingContext {

    String path();

    void next();

    JsonObject getParams();

    void fail(int statusCode);

    void fail(Throwable throwable);

    String mountPoint();

    Route currentRoute();

    int statusCode();

    boolean failed();

    Throwable failure();

    Session session();

    void setSession(Session session);

    Object userPrincipal();

    void setUserPrincipal(Object userPrincipal);

    void clearUser();

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
