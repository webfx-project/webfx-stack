package dev.webfx.stack.session.spi.impl.vertx;

import dev.webfx.platform.util.vertx.VertxInstance;
import dev.webfx.stack.session.SessionStore;
import dev.webfx.stack.session.spi.SessionServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class VertxSessionServiceProvider implements SessionServiceProvider {

    private final SessionStore sessionStore = VertxSessionStore.create(VertxInstance.getSessionStore());

    @Override
    public SessionStore getSessionStore() {
        return sessionStore;
    }
}
