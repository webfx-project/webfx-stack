package dev.webfx.stack.routing.router.auth;

import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.routing.router.auth.impl.RedirectAuthHandlerImpl;
import dev.webfx.platform.async.Handler;

/**
 * @author Bruno Salmon
 */
public interface RedirectAuthHandler extends Handler<RoutingContext> {

    static RedirectAuthHandler create(String loginPath, String unauthorizedPath, Runnable onUnnecessaryLoginHandler) {
        return new RedirectAuthHandlerImpl(loginPath, unauthorizedPath, onUnnecessaryLoginHandler);
    }
}
