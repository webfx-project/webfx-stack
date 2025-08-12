package dev.webfx.stack.cache;

/**
 * @author Bruno Salmon
 */
public record MaybeCacheValue<T> (T value, boolean fromCache, boolean sameAsCache) {}
