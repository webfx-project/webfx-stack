package dev.webfx.stack.shareddata.cache;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.shareddata.AsyncMap;
import dev.webfx.stack.shareddata.cache.spi.CachesProvider;
import dev.webfx.stack.shareddata.impl.AsyncMapConverter;

import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class Caches {

    private static CachesProvider getProvider() {
        return SingleServiceProvider.getProvider(CachesProvider.class, () -> ServiceLoader.load(CachesProvider.class));
    }

    // Supported Object values should be String, Number, Boolean, Date, as well as AST objects and arrays

    public static Future<AsyncMap<String, Object>> getCache() {
        return getProvider().getCache();
    }

    public static CacheEntry<Object> getCacheEntry(String key) {
        return getCacheEntry(key, getCache());
    }

    public static <T> CacheEntry<T> getCacheEntry(String key, Future<AsyncMap<String, T>> cache) {
        return new CacheEntryImpl<>(cache, key);
    }

    public static <T> Future<AsyncMap<String, T>> getCacheWithConversion(Function<Object, T> parser, Function<T, Object> formater) {
        return getCacheWithConversion(getCache(), parser, formater);
    }

    public static <T, U> Future<AsyncMap<String, U>> getCacheWithConversion(Future<AsyncMap<String, T>> cache, Function<T, U> parser, Function<U, T> formater) {
        return cache.map(map -> AsyncMapConverter.convertAsyncMap(map, parser, formater));
    }

}
