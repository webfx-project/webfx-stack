package dev.webfx.stack.routing.router.session;

import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.session.SessionStore;
import dev.webfx.stack.routing.router.session.impl.SessionHandlerImpl;
import dev.webfx.platform.async.Handler;
import dev.webfx.platform.util.function.Callable;

import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public interface SessionHandler extends Handler<RoutingContext> {

    static SessionHandler create(Callable<SessionStore> sessionStoreGetter, Callable<String> sessionIdFetcher, Consumer<String> sessionIdRecorder) {
        return new SessionHandlerImpl(sessionStoreGetter, sessionIdFetcher, sessionIdRecorder);
    }

    static SessionHandler create(SessionStore sessionStore, Callable<String> sessionIdFetcher, Consumer<String> sessionIdRecorder) {
        return create(() -> sessionStore, sessionIdFetcher, sessionIdRecorder);
    }
}
