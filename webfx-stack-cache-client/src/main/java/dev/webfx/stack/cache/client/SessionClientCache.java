package dev.webfx.stack.cache.client;

import dev.webfx.stack.cache.Cache;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.state.client.fx.FXSession;

/**
 * @author Bruno Salmon
 */
public class SessionClientCache implements Cache {

    @Override
    public void put(String key, Object value) {
        Session session = FXSession.getSession();
        if (session != null) {
            session.put(key, value);
            SessionService.getSessionStore().put(session);
        }
    }

    @Override
    public Object get(String key) {
        Session session = FXSession.getSession();
        return session == null ? null : session.get(key);
    }

    private static SessionClientCache INSTANCE;

    public static SessionClientCache get() {
        if (INSTANCE == null)
            INSTANCE = new SessionClientCache();
        return INSTANCE;
    }

}
