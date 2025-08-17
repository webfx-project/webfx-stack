package dev.webfx.stack.shareddata.cache;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.shareddata.AsyncMap;

/**
 * @author Bruno Salmon
 */
public class CacheEntryImpl<T> implements CacheEntry<T> {

    private final Future<AsyncMap<String, T>> cache;
    private final String key;

    public CacheEntryImpl(Future<AsyncMap<String, T>> cache, String key) {
        this.cache = cache;
        this.key = key;
    }

    @Override
    public Future<AsyncMap<String, T>> getCache() {
        return cache;
    }

    @Override
    public String getKey() {
        return key;
    }
}
