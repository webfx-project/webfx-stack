package dev.webfx.stack.shareddata.cache.localstorage;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.shareddata.AsyncMap;
import dev.webfx.stack.shareddata.ast.AstLocalMapConverter;
import dev.webfx.stack.shareddata.cache.spi.CachesProvider;
import dev.webfx.stack.shareddata.impl.LocalToAsyncMap;
import dev.webfx.stack.shareddata.storage.StorageLocalMap;
import dev.webfx.platform.storage.LocalStorage;

/**
 * @author Bruno Salmon
 */
public final class LocalStorageCachesProvider implements CachesProvider {

    private final AsyncMap<String, Object> localStorageCache = new LocalToAsyncMap<>(
        AstLocalMapConverter.convertToAstLocalMap(new StorageLocalMap(LocalStorage.getProvider()), "json"));

    @Override
    public Future<AsyncMap<String, Object>> getCache() {
        return Future.succeededFuture(localStorageCache);
    }
}
