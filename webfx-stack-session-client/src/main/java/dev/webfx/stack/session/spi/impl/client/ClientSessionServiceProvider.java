package dev.webfx.stack.session.spi.impl.client;

import dev.webfx.stack.session.SessionStore;
import dev.webfx.stack.session.spi.SessionServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class ClientSessionServiceProvider implements SessionServiceProvider {

    private final SessionStore sessionStore = new ClientSessionStore();
    @Override
    public SessionStore getSessionStore() {
        return sessionStore;
    }
}
