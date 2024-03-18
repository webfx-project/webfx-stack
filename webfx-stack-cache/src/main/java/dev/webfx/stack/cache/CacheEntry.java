package dev.webfx.stack.cache;

/**
 * @author Bruno Salmon
 */
public interface CacheEntry<T> {

    Cache getCache();

    String getKey();

    default void putValue(T value) {
        getCache().put(getKey(), value);
    }

    default T getValue() {
        return (T) getCache().get(getKey());
    }
}
