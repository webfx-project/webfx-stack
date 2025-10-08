package dev.webfx.stack.session.spi.impl.client;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.SessionStore;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
final class InMemorySessionStore implements SessionStore {

    private final Map<String, Session> sessions = new HashMap<>();

    @Override
    public Session createSession(long timeout) {
        return new InMemorySession(timeout);
    }

    @Override
    public Future<Session> get(String id) {
        Session session = sessions.get(id);
        return session != null ? Future.succeededFuture(session) : Future.failedFuture("No such session in this store");
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
