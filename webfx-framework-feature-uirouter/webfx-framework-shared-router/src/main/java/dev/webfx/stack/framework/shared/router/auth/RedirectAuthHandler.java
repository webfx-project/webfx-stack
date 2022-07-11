package dev.webfx.stack.framework.shared.router.auth;

import dev.webfx.stack.framework.shared.router.RoutingContext;
import dev.webfx.stack.framework.shared.router.auth.impl.RedirectAuthHandlerImpl;
import dev.webfx.stack.async.Handler;

/**
 * @author Bruno Salmon
 */
public interface RedirectAuthHandler extends Handler<RoutingContext> {

    static RedirectAuthHandler create(String loginPath, String unauthorizedPath) {
        return new RedirectAuthHandlerImpl(loginPath, unauthorizedPath);
    }
}
