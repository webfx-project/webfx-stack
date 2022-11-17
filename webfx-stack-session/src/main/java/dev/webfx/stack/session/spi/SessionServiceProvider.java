package dev.webfx.stack.session.spi;

import dev.webfx.stack.session.SessionStore;

/**
 * @author Bruno Salmon
 */
public interface SessionServiceProvider {

    SessionStore getSessionStore();

}
