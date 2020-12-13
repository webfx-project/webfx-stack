package dev.webfx.framework.shared.router.session;

import dev.webfx.framework.shared.router.session.impl.UserSessionHandlerImpl;
import dev.webfx.platform.shared.util.async.Handler;
import dev.webfx.framework.shared.router.RoutingContext;

/**
 * @author Bruno Salmon
 */
public interface UserSessionHandler extends Handler<RoutingContext> {

    static UserSessionHandler create() {
        return new UserSessionHandlerImpl();
    }
}
