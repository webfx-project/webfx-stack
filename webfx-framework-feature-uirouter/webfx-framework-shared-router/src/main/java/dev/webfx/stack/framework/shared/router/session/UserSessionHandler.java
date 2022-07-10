package dev.webfx.stack.framework.shared.router.session;

import dev.webfx.stack.framework.shared.router.session.impl.UserSessionHandlerImpl;
import dev.webfx.stack.platform.async.Handler;
import dev.webfx.stack.framework.shared.router.RoutingContext;

/**
 * @author Bruno Salmon
 */
public interface UserSessionHandler extends Handler<RoutingContext> {

    static UserSessionHandler create() {
        return new UserSessionHandlerImpl();
    }
}
