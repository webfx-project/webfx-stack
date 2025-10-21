package dev.webfx.stack.shareddata.cache.cachestorage.gwtj2cl;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.storage.LocalStorage;
import dev.webfx.platform.util.elemental2.async.Elemental2Async;
import dev.webfx.stack.shareddata.AsyncMap;
import elemental2.core.Global;
import elemental2.dom.*;
import jsinterop.base.Js;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Salmon
 */
final class CacheStorageAsyncMap implements AsyncMap<String, Object> {

    private static final String CACHE_NAME = "webfx-stack-orm-queries-cache"; // Temporary hardcoded cache name (= only usage for now)

    static { // Temporary code to remove old local storage cache entries
        List<String> keysToRemove = new ArrayList<>();
        LocalStorage.getKeys().forEachRemaining(key -> {
            if (key.startsWith("cache-"))
                keysToRemove.add(key);
        });
        keysToRemove.forEach(LocalStorage::removeItem);
    }

    @Override
    public Future<Object> get(String key) {
        return Elemental2Async.jsPromiseToWebFXFuture(openCache().then(cache -> cache.match(buildUrl(key))))
            .compose(response -> {
                if (response == null) {
                    return Future.succeededFuture(null);
                }
                return Elemental2Async.jsPromiseToWebFXFuture(response.text())
                    .map(text -> {
                        if (text == null) return null;
                        Object parsed = Global.JSON.parse(text);
                        // Cast JS values to Java Object (J2CL allows this as opaque objects)
                        return Js.cast(parsed);
                    });
            });
    }

    @Override
    public Future<Void> put(String key, Object value) {
        // Serialize value as JSON string; JSON.stringify handles strings, numbers, booleans, objects, arrays
        String json = Global.JSON.stringify(value);
        Request request = new Request(buildUrl(key));
        Response response = new Response(json);
        return Elemental2Async.jsPromiseToWebFXFuture(openCache().then(cache -> cache.put(request, response)))
            .mapEmpty();
    }

    @Override
    public Future<Object> remove(String key) {
        return get(key).compose(previous -> voidRemove(key).map(v -> previous));
    }

    @Override
    public Future<Void> voidRemove(String key) {
        return Elemental2Async.jsPromiseToWebFXFuture(openCache().then(cache -> cache.delete(buildUrl(key))))
            .mapEmpty();
    }

    @Override
    public Future<Void> clear() {
        CacheStorage caches = getCaches();
        if (caches == null) {
            return Future.succeededFuture();
        }
        return Elemental2Async.jsPromiseToWebFXFuture(caches.delete(CACHE_NAME))
            .mapEmpty();
    }

    private static elemental2.promise.Promise<Cache> openCache() {
        CacheStorage caches = getCaches();
        if (caches == null) {
            // Create a rejected promise to propagate the error
            return new elemental2.promise.Promise<>((resolve, reject) -> reject.onInvoke("CacheStorage is not available"));
        }
        return caches.open(CACHE_NAME);
    }

    private static CacheStorage getCaches() {
        return DomGlobal.window.caches;
    }

    private static String buildUrl(String key) { // key provided by app; assume safe for URL segment
        if (!key.startsWith("/"))
            key = "/" + key;
        return key;
    }
}
