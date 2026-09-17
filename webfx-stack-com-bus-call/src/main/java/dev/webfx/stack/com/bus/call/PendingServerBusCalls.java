package dev.webfx.stack.com.bus.call;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-side registry of the entry calls currently being processed by {@link BusCallService#listenBusEntryCalls()},
 * i.e. received from a client but whose reply hasn't been sent back yet. Each pending call is keyed by the caller
 * runId + call number (both communicated by the client), which makes it possible for a client to ask the server -
 * through the "is call pending?" endpoint (see {@link BusCallService#registerIsCallPendingEndpoint()}) - if one of
 * its own calls is still being processed (ex: a slow database query still executing), as opposed to lost. Clients
 * can use this information to extend their reply timeout instead of raising a final error while the server is still
 * working on the call.
 *
 * Entries can't leak: the reply callback in listenBusEntryCalls() always unregisters the call, and it is guaranteed
 * to be invoked, in the worst case with a failure when the internal dispatch timeout is reached (see VertxBus
 * request() method).
 *
 * Note: this class belongs to a shared (client & server) module but is only ever populated on the server (the only
 * side calling listenBusEntryCalls()). Access is synchronized in case Vert.x delivers messages of different
 * consumers on different event loop threads (a no-op when compiled to a single-threaded client platform).
 *
 * @author Bruno Salmon
 */
final class PendingServerBusCalls {

    private static final Map<String, Long> PENDING_CALL_START_TIMES = new HashMap<>();

    private PendingServerBusCalls() {}

    private static String key(String callerRunId, int callNumber) {
        return callerRunId + '#' + callNumber;
    }

    static void register(String callerRunId, int callNumber) {
        if (callerRunId != null) { // without a runId the client has no way to probe this call, so no point in tracking it
            synchronized (PENDING_CALL_START_TIMES) {
                PENDING_CALL_START_TIMES.put(key(callerRunId, callNumber), System.currentTimeMillis());
            }
        }
    }

    static void unregister(String callerRunId, int callNumber) {
        if (callerRunId != null) {
            synchronized (PENDING_CALL_START_TIMES) {
                PENDING_CALL_START_TIMES.remove(key(callerRunId, callNumber));
            }
        }
    }

    /**
     * Returns how long the given call of the given caller has been pending (in millis), or null if that call is not
     * (or no longer) pending.
     */
    static Long getPendingCallAgeMillis(String callerRunId, int callNumber) {
        if (callerRunId == null)
            return null;
        Long startTime;
        synchronized (PENDING_CALL_START_TIMES) {
            startTime = PENDING_CALL_START_TIMES.get(key(callerRunId, callNumber));
        }
        return startTime == null ? null : System.currentTimeMillis() - startTime;
    }

}
