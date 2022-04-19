package dev.webfx.framework.shared.router.auth;

import dev.webfx.framework.shared.router.RoutingContext;
import dev.webfx.framework.shared.router.auth.impl.RedirectAuthHandlerImpl;
import dev.webfx.platform.shared.async.Handler;

/**
 * @author Bruno Salmon
 */
public interface RedirectAuthHandler extends Handler<RoutingContext> {

    static RedirectAuthHandler create(String loginPath, String unauthorizedPath) {
        return new RedirectAuthHandlerImpl(loginPath, unauthorizedPath);
    }
}
