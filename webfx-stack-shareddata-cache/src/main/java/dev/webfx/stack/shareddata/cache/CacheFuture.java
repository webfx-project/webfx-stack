package dev.webfx.stack.shareddata.cache;

import dev.webfx.platform.async.Handler;
import dev.webfx.platform.async.impl.FutureImpl;
import dev.webfx.platform.scheduler.Scheduler;

/**
 * @author Bruno Salmon
 */
public class CacheFuture<T> extends FutureImpl<T> {

    private final CacheFuture<T> originalCacheFuture; // used for inUiThread() implementation (see below)

    protected Handler<T> cacheHandler;
    protected CacheAndOrSuccessWithDetailsHandler<T> cacheAndOrSuccessWithDetailsHandler;

    protected T value;
    protected boolean fromCache;
    protected boolean sameAsCache;

    public CacheFuture() {
        this(null);
    }

    public CacheFuture(CacheFuture<T> originalCacheFuture) {
        this.originalCacheFuture = originalCacheFuture != null ? originalCacheFuture : this;
    }

    @Override
    public CacheFuture<T> inUiThread() { // needs to be overridden to make it work
        CachePromise<T> promise = new CachePromise<>(this);
        onComplete(ar -> Scheduler.runInUiThread(() -> promise.handle(ar)));
        return promise.future();
    }

    public CacheFuture<T> onCache(Handler<T> handler) {
        originalCacheFuture.cacheHandler = handler;
        if (originalCacheFuture.fromCache)
            handler.handle(originalCacheFuture.value);
        return this;
    }

    public CacheFuture<T> onCacheAndOrSuccess(Handler<T> handler) {
        return onCache(handler).onSuccess(handler);
    }

    public CacheFuture<T> onCacheAndOrSuccessWithDetails(CacheAndOrSuccessWithDetailsHandler<T> handler) {
        originalCacheFuture.cacheAndOrSuccessWithDetailsHandler = handler;
        if (originalCacheFuture.value != null)
            cacheAndOrSuccessWithDetailsHandler.handle(originalCacheFuture.value, originalCacheFuture.fromCache, originalCacheFuture.sameAsCache);
        return this;
    }

    // Fluent API => bumping the return type to CacheFuture<T>

    @Override
    public CacheFuture<T> onSuccess(Handler<T> handler) {
        return (CacheFuture<T>) super.onSuccess(handler);
    }

    @Override
    public CacheFuture<T> onFailure(Handler<Throwable> handler) {
        return (CacheFuture<T>) super.onFailure(handler);
    }

    @Override
    public CacheFuture<T> onComplete(Handler<T> successHandler, Handler<Throwable> failureHandler) {
        return (CacheFuture<T>) super.onComplete(successHandler, failureHandler);
    }

    public static <T> CacheFuture<T> succeededFuture(T result) {
        CacheFuture<T> cacheFuture = new CacheFuture<>();
        cacheFuture.tryComplete(result);
        return cacheFuture;
    }
}
