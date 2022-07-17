package dev.webfx.stack.routing.router.session.impl;

import dev.webfx.stack.routing.router.session.Session;
import dev.webfx.platform.util.uuid.Uuid;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class MemorySession implements Session {

    private final String id = Uuid.randomUuid();
    private final Map<String, Object> objects = new HashMap<>();

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
}