package dev.webfx.stack.session;

import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.session.spi.SessionServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class SessionService {

    public static SessionServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(SessionServiceProvider.class, () -> ServiceLoader.load(SessionServiceProvider.class));
    }

    public static SessionStore getSessionStore() {
        return getProvider().getSessionStore();
    }

}
