package dev.webfx.stack.session;

import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface Session {

    /**
     * @return The unique ID of the session. This is generated using a random secure UUID.
     */
    String id();

    /**
     * Put some data in a session
     *
     * @param key  the key for the data
     * @param obj  the data
     * @return a reference to this, so the API can be used fluently
     */
    Session put(String key, Object obj);

    /**
     * Get some data from the session
     *
     * @param key  the key of the data
     * @return  the data
     */
    <T> T get(String key);

    /**
     * Remove some data from the session
     *
     * @param key  the key of the data
     * @return  the data that was there or null if none there
     */
    <T> T remove(String key);

    boolean isEmpty();

    long timeout();

    /**
     * Marks this session as still in use, so a store that expires idle sessions postpones its expiration.
     *
     * <p>A server session is expired on inactivity, and the only thing that can report that activity is the
     * component holding the connection it belongs to — the store itself never sees the traffic. Without this
     * call the session is expired a fixed delay after its CREATION rather than after its last use, which
     * silently drops the sessions of the clients that are still connected and using them.
     *
     * <p>No-op by default: it only means something for a store that expires sessions on idleness (the Vert.x
     * one on the server); the client-side stores keep their single session for the life of the application.
     */
    default void touch() {
    }

    default Future<Boolean> store() {
        return SessionService.getSessionStore().put(this);
    }
}
