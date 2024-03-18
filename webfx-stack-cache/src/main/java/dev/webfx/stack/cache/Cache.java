package dev.webfx.stack.cache;

/**
 * @author Bruno Salmon
 */
public interface Cache {

    void put(String key, Object value);

    Object get(String key);

    default <T> CacheEntry<T> getCacheEntry(String key) {
        return new CacheEntryImpl<>(this, key);
    }

}
