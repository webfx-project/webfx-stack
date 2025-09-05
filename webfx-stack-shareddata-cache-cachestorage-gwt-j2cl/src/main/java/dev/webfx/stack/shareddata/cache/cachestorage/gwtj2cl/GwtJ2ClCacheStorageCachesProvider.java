package dev.webfx.stack.shareddata.cache.cachestorage.gwtj2cl;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.shareddata.AsyncMap;
import dev.webfx.stack.shareddata.cache.spi.CachesProvider;

/**
 * @author Bruno Salmon
 */
public class GwtJ2ClCacheStorageCachesProvider implements CachesProvider {

    private final Future<AsyncMap<String, Object>> cache = Future.succeededFuture(new CacheStorageAsyncMap());

    @Override
    public Future<AsyncMap<String, Object>> getCache() {
        return cache;
    }

}
