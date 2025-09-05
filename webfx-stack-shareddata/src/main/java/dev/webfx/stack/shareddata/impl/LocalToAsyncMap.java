package dev.webfx.stack.shareddata.impl;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.shareddata.AsyncMap;
import dev.webfx.stack.shareddata.LocalMap;

/**
 * @author Bruno Salmon
 */
public final class LocalToAsyncMap<K, V> implements AsyncMap<K, V> {
    
    private final LocalMap<K, V> localMap;

    public LocalToAsyncMap(LocalMap<K, V> localMap) {
        this.localMap = localMap;
    }

    @Override
    public Future<V> get(K k) {
        return Future.succeededFuture(localMap.get(k));
    }

    @Override
    public Future<Void> put(K k, V v) {
        localMap.voidPut(k, v);
        return Future.succeededFuture();
    }

    @Override
    public Future<V> remove(K k) {
        return Future.succeededFuture(localMap.remove(k));
    }

    @Override
    public Future<Void> voidRemove(K k) {
        localMap.voidRemove(k);
        return Future.succeededFuture();
    }

    @Override
    public Future<Void> clear() {
        localMap.clear();
        return Future.succeededFuture();
    }

}
