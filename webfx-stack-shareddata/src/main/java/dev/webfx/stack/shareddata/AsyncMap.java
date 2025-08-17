package dev.webfx.stack.shareddata;

import dev.webfx.platform.async.Future;


/**
 * An asynchronous map.
 * <p>
 * {@link AsyncMap} does <em>not</em> allow {@code null} to be used as a key or value.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
//@VertxGen
public interface AsyncMap<K, V> {

    /**
     * Get a value from the map, asynchronously.
     *
     * @param k  the key
     * @return a future notified some time later with the async result.
     */
    Future</*@Nullable*/ V> get(K k);

    /**
     * Put a value in the map, asynchronously.
     *
     * @param k  the key
     * @param v  the value
     * @return a future notified some time later with the async result.
     */
    Future<Void> put(K k, V v);

    /**
     * Like {@link #put} but specifying a time to live for the entry. Entry will expire and get evicted after the
     * ttl.
     *
     * @param k  the key
     * @param v  the value
     * @param ttl  The time to live (in ms) for the entry
     * @return a future notified some time later with the async result.
     */
    //Future<Void> put(K k, V v, long ttl);

    /**
     * Put the entry only if there is no entry with the key already present. If key already present then the existing
     * value will be returned to the handler, otherwise null.
     *
     * @param k  the key
     * @param v  the value
     * @return a future notified some time later with the async result.
     */
    //Future</*@Nullable*/ V> putIfAbsent(K k, V v);

    /**
     * Link {@link #putIfAbsent} but specifying a time to live for the entry. Entry will expire and get evicted
     * after the ttl.
     *
     * @param k  the key
     * @param v  the value
     * @param ttl  The time to live (in ms) for the entry
     * @return a future notified some time later with the async result.
     */
    //Future</*@Nullable*/ V> putIfAbsent(K k, V v, long ttl);

    /**
     * Remove a value from the map, asynchronously.
     *
     * @param k  the key
     * @return a future notified some time later with the async result.
     */
    Future</*@Nullable*/ V> remove(K k);

    /**
     * Remove a value from the map, only if entry already exists with same value.
     *
     * @param k  the key
     * @param v  the value
     * @return a future notified some time later with the async result.
     */
    //Future<Boolean> removeIfPresent(K k, V v);

    /**
     * Replace the entry only if it is currently mapped to some value
     *
     * @param k  the key
     * @param v  the new value
     * @return a future notified some time later with the async result.
     */
    //Future</*@Nullable*/ V> replace(K k, V v);

    /**
     * Replace the entry only if it is currently mapped to some value
     *
     * @param k  the key
     * @param v  the new value
     * @param ttl  The time to live (in ms) for the entry
     * @return a future notified some time later with the previous value
     */
    /*default Future<*//*@Nullable*//* V> replace(K k, V v, long ttl) {
        return Future.failedFuture(new UnsupportedOperationException());
    }*/

    /**
     * Replace the entry only if it is currently mapped to a specific value
     *
     * @param k  the key
     * @param oldValue  the existing value
     * @param newValue  the new value
     * @return a future notified some time later with the async result.
     */
    //Future<Boolean> replaceIfPresent(K k, V oldValue, V newValue);

    /**
     * Replace the entry only if it is currently mapped to a specific value
     *
     * @param k  the key
     * @param oldValue  the existing value
     * @param newValue  the new value
     * @param ttl  The time to live (in ms) for the entry
     * @return a future notified some time later with the async result.
     */
    /*default Future<Boolean> replaceIfPresent(K k, V oldValue, V newValue, long ttl) {
        return Future.failedFuture(new UnsupportedOperationException());
    }*/

    /**
     * Clear all entries in the map
     *
     * @return a future notified some time later with the async result.
     */
    Future<Void> clear();

    /**
     * Provide the number of entries in the map
     *
     * @return a future notified some time later with the async result.
     */
    //Future<Integer> size();

    /**
     * Get the keys of the map, asynchronously.
     * <p>
     * Use this method with care as the map may contain a large number of keys,
     * which may not fit entirely in memory of a single node.
     * In this case, the invocation will result in an {@link OutOfMemoryError}.
     *
     * @return a future notified some time later with the async result.
     */
    //@GenIgnore(PERMITTED_TYPE)
    //Future<Set<K>> keys();

    /**
     * Get the values of the map, asynchronously.
     * <p>
     * Use this method with care as the map may contain a large number of values,
     * which may not fit entirely in memory of a single node.
     * In this case, the invocation will result in an {@link OutOfMemoryError}.
     *
     * @return a future notified some time later with the async result.
     */
    //@GenIgnore(PERMITTED_TYPE)
    //Future<List<V>> values();

    /**
     * Get the entries of the map, asynchronously.
     * <p>
     * Use this method with care as the map may contain a large number of entries,
     * which may not fit entirely in memory of a single node.
     * In this case, the invocation will result in an {@link OutOfMemoryError}.
     *
     * @return a future notified some time later with the async result.
     */
    //@GenIgnore
    //Future<Map<K, V>> entries();


    // WebFX additions

    Future<Void> voidRemove(K k);

}
