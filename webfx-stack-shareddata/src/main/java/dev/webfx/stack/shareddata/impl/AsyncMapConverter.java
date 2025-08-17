package dev.webfx.stack.shareddata.impl;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.shareddata.AsyncMap;

import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class AsyncMapConverter {

    public static <K, V, U> AsyncMap<K, U> convertAsyncMap(AsyncMap<K, V> asyncMap, Function<V, U> parser, Function<U, V> formater) {
        return new AsyncMap<>() {
            @Override
            public Future<U> get(K k) {
                return asyncMap.get(k).map(parser);
            }

            @Override
            public Future<Void> put(K k, U u) {
                return asyncMap.put(k, formater.apply(u));
            }

            @Override
            public Future<U> remove(K k) {
                return asyncMap.remove(k).map(parser);
            }

            @Override
            public Future<Void> clear() {
                return asyncMap.clear();
            }

            @Override
            public Future<Void> voidRemove(K k) {
                return asyncMap.voidRemove(k);
            }
        };
    }

}
