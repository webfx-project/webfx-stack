package dev.webfx.stack.shareddata.cache;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.shareddata.AsyncMap;

/**
 * @author Bruno Salmon
 */
public interface CacheEntry<T> {

    Future<AsyncMap<String, T>> getCache();

    String getKey();

    default Future<Void> putValue(T value) {
        return getCache().compose(cache -> cache.put(getKey(), value)).mapEmpty();
    }

    default Future<T> getValue() {
        return getCache().compose(cache -> cache.get(getKey()));
    }
}
