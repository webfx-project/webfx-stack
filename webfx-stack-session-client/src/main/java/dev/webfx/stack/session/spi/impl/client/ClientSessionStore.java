package dev.webfx.stack.session.spi.impl.client;

import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.SessionStore;
import dev.webfx.platform.async.Future;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
final class ClientSessionStore implements SessionStore {

    private final Map<String, Session> sessions = new HashMap<>();

    @Override
    public Session createSession() {
        return new ClientSession();
    }

    @Override
    public Future<Session> get(String id) {
        return Future.succeededFuture(sessions.get(id));
    }

    @Override
    public Future<Boolean> delete(String id) {
        return Future.succeededFuture(sessions.remove(id) != null);
    }

    @Override
    public Future<Boolean> put(Session session) {
        sessions.put(session.id(), session);
        return Future.succeededFuture(true);
    }

    @Override
    public Future<Boolean> clear() {
        sessions.clear();
        return Future.succeededFuture(true);
    }
}
