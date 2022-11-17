package dev.webfx.stack.session.spi.impl.client;

import dev.webfx.platform.util.uuid.Uuid;
import dev.webfx.stack.session.Session;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
final class ClientSession implements Session {

    private final String id;
    private final Map<String, Object> objects = new HashMap<>();

    public ClientSession() {
        this(Uuid.randomUuid());
    }

    public ClientSession(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Session put(String key, Object obj) {
        objects.put(key, obj);
        return this;
    }

    @Override
    public <T> T get(String key) {
        return (T) objects.get(key);
    }

    @Override
    public <T> T remove(String key) {
        return (T) objects.remove(key);
    }

    @Override
    public boolean isEmpty() {
        return objects.isEmpty();
    }
}
