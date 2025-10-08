package dev.webfx.stack.session.spi.impl.vertx;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.vertx.VertxAsync;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.SessionStore;

/**
 * @author Bruno Salmon
 */
final class VertxSessionStore implements SessionStore {

    private final io.vertx.ext.web.sstore.SessionStore vertxSessionStore;

    private VertxSessionStore(io.vertx.ext.web.sstore.SessionStore vertxSessionStore) {
        this.vertxSessionStore = vertxSessionStore;
    }

    static VertxSessionStore create(io.vertx.ext.web.sstore.SessionStore vertxSessionStore) {
        return new VertxSessionStore(vertxSessionStore);
    }

    @Override
    public Session createSession(long timeout) {
        return VertxSession.create(vertxSessionStore.createSession(timeout));
    }

    @Override
    public Future<Session> get(String id) {
        return VertxAsync.toWebfxFuture(vertxSessionStore.get(id))
            .map(VertxSession::create);
    }

    @Override
    public Future<Boolean> delete(String id) {
       return VertxAsync.toWebfxFuture(vertxSessionStore.delete(id))
           .map(v -> true);
    }

    @Override
    public Future<Boolean> put(Session session) {
        return VertxAsync.toWebfxFuture(vertxSessionStore.put(((VertxSession) session).getVertxSession()))
            .map(v -> true);
    }

    @Override
    public Future<Boolean> clear() {
        return VertxAsync.toWebfxFuture(vertxSessionStore.clear())
            .map(v -> true);
    }
}
