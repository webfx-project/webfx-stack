package dev.webfx.stack.shareddata.impl;

import dev.webfx.stack.shareddata.LocalMap;

import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class LocalMapConverter {

    public static <K, V, U> LocalMap<K, U> convertLocalMap(LocalMap<K, V> localMap, Function<V, U> parser, Function<U, V> formater) {
        return new LocalMap<>() {

            @Override
            public U get(Object key) {
                return parser.apply(localMap.get(key));
            }

            @Override
            public U put(K key, U value) {
                U previousValue = get(key);
                voidPut(key, value);
                return previousValue;
            }

            @Override
            public void voidPut(K key, U value) {
                localMap.voidPut(key, formater.apply(value));
            }

            @Override
            public U remove(Object key) {
                return parser.apply(localMap.remove(key));
            }

            @Override
            public void voidRemove(K key) {
                localMap.voidRemove(key);
            }

            @Override
            public void clear() {
                localMap.clear();
            }

            @Override
            public int size() {
                return localMap.size();
            }

            @Override
            public boolean isEmpty() {
                return localMap.isEmpty();
            }

            @Override
            public void close() {
                localMap.close();
            }
        };
    }

}
