package dev.webfx.stack.session.spi.impl.vertx;

import dev.webfx.stack.session.Session;

import java.util.Map;

/**
 * @author Bruno Salmon
 */
public class VertxSession implements Session {

    private final io.vertx.ext.web.Session vertxSession;

    private VertxSession(io.vertx.ext.web.Session vertxSession) {
        this.vertxSession = vertxSession;
    }

    public static VertxSession create(io.vertx.ext.web.Session vertxSession) {
        if (vertxSession == null)
            return null;
        Map<String, Object> data = vertxSession.data();
        Object cachedSession = data.get("$webfxSession");
        if (cachedSession instanceof VertxSession)
            return (VertxSession) cachedSession;
        VertxSession webfxSession = new VertxSession(vertxSession);
        data.put("$webfxSession", webfxSession);
        return webfxSession;
    }

    public io.vertx.ext.web.Session getVertxSession() {
        return vertxSession;
    }

    @Override
    public String id() {
        return vertxSession.id();
    }

    @Override
    public Session put(String key, Object obj) {
        vertxSession.put(key, obj);
        return this;
    }

    @Override
    public <T> T get(String key) {
        return vertxSession.get(key);
    }

    @Override
    public <T> T remove(String key) {
        return vertxSession.remove(key);
    }

    @Override
    public boolean isEmpty() {
        return vertxSession.isEmpty();
    }

    @Override
    public long timeout() {
        return vertxSession.timeout();
    }
}
