package dev.webfx.stack.shareddata.impl;

import dev.webfx.stack.shareddata.LocalMap;

import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class MapToLocalMap<K, V> implements LocalMap<K, V> {

    private final Map<K, V> map;

    public MapToLocalMap(Map<K, V> map) {
        this.map = map;
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void close() {
        clear();
    }
}
