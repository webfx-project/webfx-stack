package dev.webfx.stack.shareddata.cache.serial;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.shareddata.AsyncMap;
import dev.webfx.stack.shareddata.cache.CacheEntry;
import dev.webfx.stack.shareddata.cache.Caches;
import dev.webfx.stack.com.serial.SerialCodecManager;

/**
 * @author Bruno Salmon
 */
public final class SerialCache {

    private static final Future<AsyncMap<String, Object>> SERIAL_CACHE = Caches.getCacheWithConversion(
        SerialCodecManager::decodeFromJson, SerialCodecManager::encodeToJson);

    public static <T> CacheEntry<T> createCacheEntry(String cacheEntryKey) {
        return (CacheEntry<T>) Caches.getCacheEntry(cacheEntryKey, SERIAL_CACHE);
    }

}
