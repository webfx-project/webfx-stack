package dev.webfx.stack.db.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Process-wide "analyze on next occurrence" registry backing the /monitor slow-query drill-down.
 * <p>
 * An admin arms a tracked read statement; the next time the executor runs that exact statement it
 * captures the <em>real</em> parameters, runs {@code EXPLAIN (ANALYZE, BUFFERS)} on a separate
 * connection, and stores the plan here. The /monitor page surfaces the armed + captured entries in
 * its regular snapshot ({@link #snapshotAll}), so an operator gets a production query plan with
 * production parameters without direct DB access.
 * <p>
 * Arms <b>persist until captured or explicitly removed</b> ({@link #remove} for one, {@link #clearAll}
 * for all — the /monitor "Clear" button) — there is no time-based expiry, so a query that runs only
 * rarely is still captured whenever it finally executes. A {@link #MAX_ARMS} cap (oldest evicted)
 * bounds memory.
 * <p>
 * Client-safe on purpose (no executor / Vert.x dependency): the executor supplies the finished plan
 * text; this only holds arming state + captured results. Access is {@code synchronized} (a no-op on
 * single-threaded J2CL, consistent across the server's event loops on the JVM). A {@code volatile}
 * armed count lets the hot query path skip the map lookup entirely when nothing is armed —
 * {@link #claimIfArmed} re-checks under the lock, so a stale fast-path read is harmless.
 *
 * @author Bruno Salmon
 */
public final class SqlAnalyzeRegistry {

    /** NONE = nothing armed/captured; PENDING = armed or capturing; READY = plan captured. */
    public enum Status { NONE, PENDING, READY }

    private static final SqlAnalyzeRegistry INSTANCE = new SqlAnalyzeRegistry();

    public static SqlAnalyzeRegistry get() {
        return INSTANCE;
    }

    /** Cap on stored captured results; past this, the oldest is evicted. */
    private static final int MAX_RESULTS = 32;

    /** Cap on live (uncaptured) arms; past this, the oldest arm is evicted. */
    private static final int MAX_ARMS = 64;

    // Fast-path hint read without locking (see class doc); mutated only under `this`.
    private volatile int armedCount;
    private final Map<String, Long> armedAt = new HashMap<>();     // statement -> arm epoch ms (no expiry)
    private final Map<String, Result> results = new HashMap<>();   // statement -> captured plan / capturing marker

    private SqlAnalyzeRegistry() {}

    /**
     * Arms a statement for analyze-on-next-occurrence, clearing any prior arm/result for it. The arm
     * persists until the statement next runs (captured) or it is {@link #remove removed}/{@link #clearAll cleared}.
     */
    public synchronized void arm(String statement, long nowMillis) {
        results.remove(statement);       // discard any stale prior plan
        armedAt.put(statement, nowMillis);
        if (armedAt.size() > MAX_ARMS)
            evictOldestArm();
        armedCount = armedAt.size();
    }

    /** Unsynchronized fast-path hint for the executor: is anything armed at all? */
    public boolean hasArmed() {
        return armedCount > 0;
    }

    /**
     * If {@code statement} is armed, atomically disarm it and return true so exactly one execution
     * captures it; the caller then runs EXPLAIN and calls {@link #storeResult}. A capturing marker is
     * recorded so {@link #getResult}/{@link #snapshotAll} report PENDING until the plan lands.
     */
    public synchronized boolean claimIfArmed(String statement, long nowMillis) {
        if (armedAt.remove(statement) == null) {
            armedCount = armedAt.size();
            return false; // not armed — don't capture
        }
        armedCount = armedAt.size();
        results.put(statement, new Result(statement, Status.PENDING, null, null, null, null, nowMillis));
        return true;
    }

    /**
     * Stores the captured EXPLAIN plan for a claimed statement, along with the parameters used and
     * the original DQL statement it was compiled from (null when the SQL wasn't DQL-derived).
     */
    public synchronized void storeResult(String statement, String dqlStatement, String parametersDisplay, String planText, String clientVersion, long nowMillis) {
        if (results.size() >= MAX_RESULTS && !results.containsKey(statement))
            evictOldestResult();
        results.put(statement, new Result(statement, Status.READY, dqlStatement, parametersDisplay, planText, clientVersion, nowMillis));
    }

    /** Current analyze state for a statement: READY (plan), PENDING (armed/capturing) or NONE. */
    public synchronized Result getResult(String statement, long nowMillis) {
        Result r = results.get(statement);
        if (r != null)
            return r;
        if (armedAt.containsKey(statement))
            return new Result(statement, Status.PENDING, null, null, null, null, armedAt.get(statement));
        return NONE;
    }

    /** Drops one statement's arm and captured result (the /monitor per-row "Reset"). */
    public synchronized void remove(String statement) {
        armedAt.remove(statement);
        results.remove(statement);
        armedCount = armedAt.size();
    }

    /** Drops every arm and captured result (the /monitor "Clear" button). */
    public synchronized void clearAll() {
        armedAt.clear();
        results.clear();
        armedCount = 0;
    }

    /**
     * A snapshot of all currently armed (PENDING) and captured (READY) statements, for the /monitor
     * page to render — each {@link Result} carries its own {@code statement}. Captured results win
     * over the arm marker for the same statement (there is at most one entry per statement).
     */
    public synchronized List<Result> snapshotAll() {
        List<Result> all = new ArrayList<>(results.size() + armedAt.size());
        all.addAll(results.values());
        for (Map.Entry<String, Long> e : armedAt.entrySet())
            if (!results.containsKey(e.getKey()))
                all.add(new Result(e.getKey(), Status.PENDING, null, null, null, null, e.getValue()));
        return all;
    }

    private void evictOldestResult() {
        String oldest = null;
        long min = Long.MAX_VALUE;
        for (Map.Entry<String, Result> e : results.entrySet())
            if (e.getValue().atMillis < min) {
                min = e.getValue().atMillis;
                oldest = e.getKey();
            }
        if (oldest != null)
            results.remove(oldest);
    }

    private void evictOldestArm() {
        String oldest = null;
        long min = Long.MAX_VALUE;
        for (Map.Entry<String, Long> e : armedAt.entrySet())
            if (e.getValue() < min) {
                min = e.getValue();
                oldest = e.getKey();
            }
        if (oldest != null)
            armedAt.remove(oldest);
    }

    private static final Result NONE = new Result(null, Status.NONE, null, null, null, null, 0);

    /**
     * Immutable analyze state for one {@code statement}. PENDING while waiting for the next occurrence
     * or capturing the plan; READY carries {@code planText}, the {@code parametersDisplay} used, the
     * {@code dqlStatement} the SQL was compiled from (null when not DQL-derived) and {@code atMillis} =
     * the capture time. NONE means nothing armed/captured.
     */
    public static final class Result {
        public final String statement;         // the analyzed statement (null only for the NONE sentinel)
        public final Status status;
        public final String dqlStatement;      // original DQL, null unless READY and DQL-derived
        public final String parametersDisplay; // null unless READY
        public final String planText;          // null unless READY
        public final String clientVersion;     // client build version that ran the captured occurrence; null unless READY (and known)
        public final long atMillis;            // capture time (READY) or arm time (PENDING) — for age + oldest-eviction

        Result(String statement, Status status, String dqlStatement, String parametersDisplay, String planText, String clientVersion, long atMillis) {
            this.statement = statement;
            this.status = status;
            this.dqlStatement = dqlStatement;
            this.parametersDisplay = parametersDisplay;
            this.planText = planText;
            this.clientVersion = clientVersion;
            this.atMillis = atMillis;
        }
    }
}
