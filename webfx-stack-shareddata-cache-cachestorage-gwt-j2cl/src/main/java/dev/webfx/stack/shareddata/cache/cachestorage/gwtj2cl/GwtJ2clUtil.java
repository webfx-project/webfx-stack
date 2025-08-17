package dev.webfx.stack.shareddata.cache.cachestorage.gwtj2cl;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;

/**
 * @author Bruno Salmon
 */
final class GwtJ2clUtil { // Duplicate with the one in the Fetch API TODO: centralize this

    // Utility: convert JS Promise<T> to WebFX Future<T>
    static <T> Future<T> jsPromiseToWebFXFuture(elemental2.promise.Promise<T> jsPromise) {
        Promise<T> p = Promise.promise();
        jsPromise
            .then(obj -> {
                p.tryComplete(obj);
                return null;
            })
            .catch_(error -> {
                if (error instanceof Throwable)
                    p.tryFail((Throwable) error);
                else
                    p.tryFail(error.toString());
                return null;
            });
        return p.future();
    }
}
