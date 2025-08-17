package dev.webfx.stack.shareddata.cache;

import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.impl.Listener;
import dev.webfx.platform.async.impl.PromiseInternal;

/**
 * @author Bruno Salmon
 */
public final class CachePromise<T> extends CacheFuture<T> implements PromiseInternal<T>, Listener<T> {

    public CachePromise() {
    }

    public CachePromise(CacheFuture<T> delegate) {
        super(delegate);
    }

    public void handle(AsyncResult<T> ar) {
        if (ar.succeeded()) {
            onSuccess(ar.result());
        } else {
            onFailure(ar.cause());
        }
    }

    @Override
    public void onSuccess(T value) {
        tryComplete(value);
        if (cacheAndOrSuccessWithDetailsHandler != null)
            cacheAndOrSuccessWithDetailsHandler.handle(value, fromCache, sameAsCache);
    }

    @Override
    public void onFailure(Throwable failure) {
        tryFail(failure);
    }

    public void emitCacheValue(T value) {
        emitValue(value, true, false);
    }

    public void emitSuccessValue(T value, boolean sameAsCache) {
        emitValue(value, false, sameAsCache);
    }

    public void emitValue(T value, boolean fromCache, boolean sameAsCache) {
        this.value = value;
        this.fromCache = fromCache;
        this.sameAsCache = sameAsCache;
        if (fromCache) {
            if (cacheHandler != null)
                cacheHandler.handle(value);
            if (cacheAndOrSuccessWithDetailsHandler != null)
                cacheAndOrSuccessWithDetailsHandler.handle(value, true, sameAsCache);
        } else
            onSuccess(value);
    }

    @Override
    public CacheFuture<T> future() {
        return this;
    }

}
