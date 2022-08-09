package dev.webfx.stack.routing.router.session;

import dev.webfx.stack.routing.router.session.impl.UserSessionHandlerImpl;
import dev.webfx.platform.async.Handler;
import dev.webfx.stack.routing.router.RoutingContext;

/**
 * @author Bruno Salmon
 */
public interface UserSessionHandler extends Handler<RoutingContext> {

    static UserSessionHandler create() {
        return new UserSessionHandlerImpl();
    }
}
