package dev.webfx.stack.shareddata.cache;

/**
 * @author Bruno Salmon
 */
public interface CacheAndOrSuccessWithDetailsHandler<T> {

    void handle(T value, boolean fromCache, boolean sameAsCache);

}
