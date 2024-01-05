package dev.webfx.stack.cache;

/**
 * @author Bruno Salmon
 */
public class CacheEntryImpl<T> implements CacheEntry<T> {

    private final Cache cache;
    private final String key;

    public CacheEntryImpl(Cache cache, String key) {
        this.cache = cache;
        this.key = key;
    }

    @Override
    public Cache getCache() {
        return cache;
    }

    @Override
    public String getKey() {
        return key;
    }
}
