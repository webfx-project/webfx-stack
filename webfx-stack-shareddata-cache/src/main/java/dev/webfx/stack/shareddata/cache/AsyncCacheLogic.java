package dev.webfx.stack.shareddata.cache;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.util.tuples.Pair;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * @author Bruno Salmon
 */
public final class AsyncCacheLogic {

    public static <T, A, R> CacheFuture<T> executeAsyncCacheLogic(CacheEntry<Pair<A, R>> cacheEntry, AsyncFunction<A, R> asyncFunction, A argument, Function<R, T> resultMapper) {
        return executeAsyncCacheLogic(cacheEntry, asyncFunction.apply(argument), argument, resultMapper);
    }

    public static <T, A, R> CacheFuture<T> executeAsyncCacheLogic(CacheEntry<Pair<A, R>> cacheEntry, Future<R> noCacheFuture, A argument, Function<R, T> resultMapper) {
        CachePromise<T> promise = new CachePromise<>();
        Future<R> cacheFuture = cacheEntry == null ? Future.succeededFuture() : cacheEntry.getValue()
            .map(pair -> {
                R result = null;
                if (pair != null) {
                    try {
                        if (!noCacheFuture.isComplete() && Objects.equals(argument, pair.get1())) {
                            result = pair.get2();
                            if (result != null) {
                                Console.log("Restoring cache '" + cacheEntry.getKey() + "'");
                                T finalResult = resultMapper.apply(result);
                                promise.emitCacheValue(finalResult);
                            }
                        } else
                            Console.log("Cache for '" + cacheEntry.getKey() + "' can't be used, as its argument was different: " + pair.get1());
                    } catch (Exception e) {
                        Console.log("WARNING: Restoring '" + cacheEntry.getKey() + "' cache failed: " + e.getMessage());
                    }
                }
                return result;
            });
        noCacheFuture
            .onFailure(promise::fail)
            .onSuccess(result -> {
                T finalResult = resultMapper.apply(result);
                boolean sameAsCache = false;
                if (cacheEntry != null && cacheFuture.isComplete()) {
                    sameAsCache = Objects.equals(result, cacheFuture.result());
                    if (!sameAsCache)
                        cacheEntry.putValue(new Pair<>(argument, result));
                }
                promise.emitSuccessValue(finalResult, sameAsCache);
            });
        return promise.future();
    }

    public static <T, A, R> CacheFuture<T[]> executeAsyncBatchCacheLogic(CacheEntry<Pair<A[], R[]>> cacheEntry, Future<Batch<R>> noCacheFuture, Batch<A> argumentsBatch, BiFunction<Integer, R, T> resultMapper, IntFunction<T[]> generator) {
        A[] arguments = argumentsBatch.getArray();
        CachePromise<T[]> promise = new CachePromise<>();
        // Using Object[] instead of R[] because the cache value is deserialized as Object[]
        Future<Object[]> cacheFuture = cacheEntry == null ? Future.succeededFuture() : cacheEntry.getValue()
            .map(pair -> {
                Object[] results = null;
                if (pair != null) {
                    try {
                        if (!noCacheFuture.isComplete() && Objects.deepEquals(arguments, pair.get1())) {
                            results = pair.get2();
                            if (results != null) {
                                Console.log("Restoring cache '" + cacheEntry.getKey() + "'");
                                T[] finalResults = Arrays.map(results, (i, r) -> resultMapper.apply(i, (R) r), generator);
                                promise.emitCacheValue(finalResults);
                            }
                        } else
                            Console.log("Cache for '" + cacheEntry.getKey() + "' can't be used, as its argument was different: " + pair.get1());
                    } catch (Exception e) {
                        Console.log("WARNING: Restoring '" + cacheEntry.getKey() + "' cache failed: " + e.getMessage());
                    }
                }
                return results;
            });
        noCacheFuture
            .onFailure(promise::fail)
            .onSuccess(batch -> {
                R[] results = batch.getArray();
                T[] finalResult = Arrays.map(results, resultMapper, generator);
                boolean sameAsCache = false;
                if (cacheEntry != null && cacheFuture.isComplete()) {
                    sameAsCache = Objects.deepEquals(results, cacheFuture.result());
                    if (!sameAsCache)
                        cacheEntry.putValue(new Pair<>(arguments, results));
                }
                promise.emitSuccessValue(finalResult, sameAsCache);
            });
        return promise.future();
    }
}
