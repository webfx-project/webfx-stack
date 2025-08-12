package dev.webfx.stack.cache;

/**
 * @author Bruno Salmon
 */
public final class DefaultCache {

    private static Cache DEFAULT_CACHE = new NoCache();

    public static Cache getDefaultCache() {
        return DEFAULT_CACHE;
    }

    public static void setDefaultCache(Cache defaultCache) {
        DEFAULT_CACHE = defaultCache;
    }

    public static <T> CacheEntry<T> getDefaultCacheEntry(String key) {
        return getDefaultCache().getCacheEntry(key);
    }

}
