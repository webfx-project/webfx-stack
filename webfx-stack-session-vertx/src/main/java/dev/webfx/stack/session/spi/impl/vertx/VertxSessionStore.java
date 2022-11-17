package dev.webfx.stack.session.spi.impl.vertx;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.SessionStore;

/**
 * @author Bruno Salmon
 */
class VertxSessionStore implements SessionStore {

    private final io.vertx.ext.web.sstore.SessionStore vertxSessionStore;

    private VertxSessionStore(io.vertx.ext.web.sstore.SessionStore vertxSessionStore) {
        this.vertxSessionStore = vertxSessionStore;
    }

    static VertxSessionStore create(io.vertx.ext.web.sstore.SessionStore vertxSessionStore) {
        return new VertxSessionStore(vertxSessionStore);
    }

    @Override
    public Session createSession() {
        return VertxSession.create(vertxSessionStore.createSession(Long.MAX_VALUE));
    }

    @Override
    public Future<Session> get(String id) {
        Promise<Session> promise = Promise.promise();
        vertxSessionStore.get(id)
                .onFailure(promise::fail)
                .onSuccess(s -> promise.complete(VertxSession.create(s)));
        return promise.future();
    }

    @Override
    public Future<Boolean> delete(String id) {
        Promise<Boolean> promise = Promise.promise();
        vertxSessionStore.delete(id)
                .onFailure(promise::fail)
                .onSuccess(s -> promise.complete(true));
        return promise.future();
    }

    @Override
    public Future<Boolean> put(Session session) {
        Promise<Boolean> promise = Promise.promise();
        vertxSessionStore.put(((VertxSession) session).getVertxSession())
                .onFailure(promise::fail)
                .onSuccess(s -> promise.complete(true));
        return promise.future();
    }

    @Override
    public Future<Boolean> clear() {
        Promise<Boolean> promise = Promise.promise();
        vertxSessionStore.clear()
                .onFailure(promise::fail)
                .onSuccess(s -> promise.complete(true));
        return promise.future();
    }
}
