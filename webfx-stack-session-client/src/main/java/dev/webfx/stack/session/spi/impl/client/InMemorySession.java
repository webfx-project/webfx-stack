package dev.webfx.stack.session.spi.impl.client;

import dev.webfx.platform.util.uuid.Uuid;
import dev.webfx.stack.session.Session;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
final class InMemorySession implements Session {

    private final String id;
    final Map<String, Object> values = new HashMap<>();

    public InMemorySession() {
        this(Uuid.randomUuid());
    }

    InMemorySession(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Session put(String key, Object obj) {
        values.put(key, obj);
        return this;
    }

    @Override
    public <T> T get(String key) {
        return (T) values.get(key);
    }

    @Override
    public <T> T remove(String key) {
        return (T) values.remove(key);
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }
}
