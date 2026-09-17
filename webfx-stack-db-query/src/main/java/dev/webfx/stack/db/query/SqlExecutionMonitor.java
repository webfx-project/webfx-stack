package dev.webfx.stack.db.query;

import dev.webfx.platform.async.util.AsyncQueue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Process-wide monitoring of local SQL execution — reads (queries / SELECT) and writes
 * (submits / INSERT-UPDATE-DELETE) tracked separately — for the /monitor page.
 * <p>
 * Holds per-kind cumulative counters (count, total time, errors) plus a reference to each
 * kind's {@link AsyncQueue} so the snapshot can read live queue depth. The concrete executor
 * ({@code VertxLocalPostgresQuerySubmitServiceProvider}) registers its queue and calls
 * {@link #record} on each completed operation; the query-push {@code getMonitorInfo()} reads
 * {@link #snapshot()}.
 * <p>
 * Client-safe on purpose (no executor / Vert.x dependency): it can sit next to the query
 * facade and be read from the monitor aggregation without coupling that to the executor. Access
 * is {@code synchronized} — a no-op on single-threaded J2CL clients (where it is never written)
 * and consistent across the server's event loops on the JVM.
 *
 * @author Bruno Salmon
 */
public final class SqlExecutionMonitor {

    /** READ = query (SELECT); WRITE = submit (INSERT/UPDATE/DELETE). */
    public enum Kind { READ, WRITE }

    private static final SqlExecutionMonitor INSTANCE = new SqlExecutionMonitor();

    public static SqlExecutionMonitor get() {
        return INSTANCE;
    }

    private final KindCounters read = new KindCounters();
    private final KindCounters write = new KindCounters();

    private SqlExecutionMonitor() {}

    private KindCounters counters(Kind kind) {
        return kind == Kind.WRITE ? write : read;
    }

    /** Registers a kind's AsyncQueue so {@link #snapshot()} can read its live depth. */
    public synchronized void registerQueue(Kind kind, AsyncQueue queue) {
        counters(kind).queue = queue;
    }

    /**
     * Records one completed SQL operation of the given kind.
     *
     * @param nanos   wall time of the operation
     * @param success whether it completed without error
     */
    public synchronized void record(Kind kind, long nanos, boolean success) {
        KindCounters c = counters(kind);
        c.count++;
        c.totalNanos += nanos;
        if (!success)
            c.errorCount++;
    }

    /**
     * Records the detail of one failed SQL operation for the /monitor "Errors" drill-down (in addition
     * to the {@link #record} counter bump). Kept in a small bounded ring buffer — the newest
     * {@link #MAX_RECENT_ERRORS} are retained, older ones evicted — so the count on the card can
     * exceed the number of rows shown. {@code message} is truncated to bound the payload; {@code
     * epochMillis} is the caller's wall-clock time (passed in to keep this module clock-free).
     */
    public synchronized void recordError(Kind kind, String statement, String message, Boolean backoffice, long epochMillis) {
        recentErrors.addLast(new ErrorEntry(epochMillis, kind, statement, truncate(message, MAX_ERROR_MESSAGE_CHARS), backoffice));
        while (recentErrors.size() > MAX_RECENT_ERRORS)
            recentErrors.removeFirst();
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : s;
    }

    /**
     * Resets the cumulative counters and per-statement rollup so the /monitor page can measure a
     * fresh window (Clear → run an action → see only what it triggered). Keeps live state: the
     * registered {@link AsyncQueue}s and the in-flight registry (queries still running) are
     * untouched; queue high-water marks live on the queues, not here.
     */
    public synchronized void reset() {
        read.count = 0;  read.totalNanos = 0;  read.errorCount = 0;
        write.count = 0; write.totalNanos = 0; write.errorCount = 0;
        statementStats.clear();
        recentErrors.clear();
    }

    // ── In-flight registry + per-statement rollup (drives the /monitor drill-down and,
    //    via the stored cancel handle, a future query-cancellation feature) ──────────────

    /** Default number of top statements returned <em>per (kind × metric)</em> by {@link #snapshot()}. */
    public static final int DEFAULT_TOP_STATEMENTS = 20;

    /**
     * Minimum executions before a statement is eligible for the avg/max rankings. A single cold-cache
     * or one-off execution can be arbitrarily slow; requiring a few runs keeps the "slowest" views to
     * <em>chronically</em> slow statements rather than one-time outliers. The by-total ranking has no
     * such floor (it's about accumulated time, where a rare statement can't dominate anyway).
     */
    private static final int MIN_RANKED_COUNT = 3;

    /** Cap on distinct statements tracked; past this, an unprotected entry is evicted (see {@link #evictStatement()}). */
    private static final int MAX_TRACKED_STATEMENTS = 512;

    /**
     * On eviction, the slowest-by-max statements are protected so the avg/max drill-down stays
     * reliable: a slow-but-rare query has a small <em>total</em> time and would otherwise be the first
     * evicted — losing exactly the query an admin is hunting. We keep this many slowest-by-max and
     * evict the smallest-total among the rest.
     */
    private static final int EVICTION_PROTECTED_BY_MAX = 64;

    /** Newest failed operations retained for the "Errors" drill-down (older ones evicted). */
    private static final int MAX_RECENT_ERRORS = 50;

    /** Cap on a stored error message (Postgres messages can be long); keeps the wire payload bounded. */
    private static final int MAX_ERROR_MESSAGE_CHARS = 1000;

    private long idSeq;
    private final Map<Long, InFlight> inFlight = new HashMap<>();
    private final Map<String, StatementStat> statementStats = new HashMap<>();
    // Bounded FIFO of recent failures (oldest at the head, newest at the tail); read newest-first.
    private final ArrayDeque<ErrorEntry> recentErrors = new ArrayDeque<>();

    /**
     * Registers a starting SQL execution and returns its monitor id. The {@code cancelHandle} is
     * stored opaquely — the executor supplies a client-safe {@link Runnable} cancel action (so this
     * module stays free of any Vert.x dependency), which {@link #cancel(long)} runs on request;
     * pass null when none is available.
     */
    public synchronized long onStart(Kind kind, String statement, Object cancelHandle, Boolean backoffice) {
        long id = ++idSeq;
        inFlight.put(id, new InFlight(id, kind, statement, System.nanoTime(), cancelHandle, backoffice));
        return id;
    }

    /** Marks a SQL execution finished: drops it from in-flight and rolls its time into the per-statement stats. */
    public synchronized void onEnd(long id) {
        InFlight f = inFlight.remove(id);
        if (f == null)
            return;
        long nanos = System.nanoTime() - f.startNanos;
        StatementStat stat = statementStats.get(f.statement);
        if (stat == null) {
            if (statementStats.size() >= MAX_TRACKED_STATEMENTS)
                evictStatement();
            statementStats.put(f.statement, stat = new StatementStat(f.kind));
        }
        stat.count++;
        stat.totalNanos += nanos;
        if (nanos > stat.maxNanos)
            stat.maxNanos = nanos;
        // Accumulate the caller's origin (BO/FO) — a statement run from both sides reports "both".
        if (f.backoffice != null) {
            if (f.backoffice)
                stat.sawBackoffice = true;
            else
                stat.sawFrontoffice = true;
        }
    }

    /** Returns the opaque cancel handle for an in-flight query, or null if it already finished. */
    public synchronized Object cancelHandleOf(long id) {
        InFlight f = inFlight.get(id);
        return f == null ? null : f.cancelHandle;
    }

    /**
     * Whether {@code statement} is a read (query/SELECT) statement this monitor has seen — i.e. it
     * is in the per-statement rollup or currently in-flight as a READ. Used to gate "analyze this
     * statement" requests to statements the server actually runs, so an admin can never arm analysis
     * of arbitrary client-supplied SQL.
     */
    public synchronized boolean isKnownReadStatement(String statement) {
        if (statement == null)
            return false;
        StatementStat stat = statementStats.get(statement);
        if (stat != null)
            return stat.kind == Kind.READ;
        for (InFlight f : inFlight.values())
            if (f.kind == Kind.READ && statement.equals(f.statement))
                return true;
        return false;
    }

    /**
     * Best-effort cancellation of an in-flight query: runs its stored cancel action outside this
     * monitor's lock (the action may open a network connection — never hold the lock across it).
     * The executor supplies the handle as a client-safe {@link Runnable}, so this stays free of any
     * executor / Vert.x dependency. Returns true if an action was found and run, false if the id is
     * unknown, already finished, or had no cancel handle.
     */
    public boolean cancel(long id) {
        Object handle;
        synchronized (this) {
            InFlight f = inFlight.get(id);
            handle = f == null ? null : f.cancelHandle;
        }
        if (handle instanceof Runnable r) {
            r.run();
            return true;
        }
        return false;
    }

    /**
     * Evicts one statement to make room, PROTECTING the slowest-by-max so the avg/max drill-down stays
     * reliable. A slow-but-rare query has a small total time and, under a naive smallest-total eviction,
     * would be the first dropped — losing exactly the query an admin is hunting. So we keep the
     * {@link #EVICTION_PROTECTED_BY_MAX} slowest-by-max and evict the smallest-total among the rest
     * (falling back to the smallest-total overall in the corner case where everything is protected).
     */
    private void evictStatement() {
        Set<String> protectedByMax = topStatementKeysByMax(EVICTION_PROTECTED_BY_MAX);
        String victim = smallestTotalKey(protectedByMax);
        if (victim == null) // everything is protected (cap ≤ protected size) — evict smallest-total overall
            victim = smallestTotalKey(null);
        if (victim != null)
            statementStats.remove(victim);
    }

    /** The key of the smallest-total statement not in {@code exclude} (null → consider all), or null. */
    private String smallestTotalKey(Set<String> exclude) {
        String smallest = null;
        long min = Long.MAX_VALUE;
        for (Map.Entry<String, StatementStat> e : statementStats.entrySet()) {
            if (exclude != null && exclude.contains(e.getKey()))
                continue;
            if (e.getValue().totalNanos < min) {
                min = e.getValue().totalNanos;
                smallest = e.getKey();
            }
        }
        return smallest;
    }

    /** The keys of the {@code n} statements with the largest max time (the ones to protect from eviction). */
    private Set<String> topStatementKeysByMax(int n) {
        List<Map.Entry<String, StatementStat>> entries = new ArrayList<>(statementStats.entrySet());
        entries.sort(Comparator.comparingLong((Map.Entry<String, StatementStat> e) -> e.getValue().maxNanos).reversed());
        Set<String> keys = new HashSet<>();
        int limit = Math.min(Math.max(0, n), entries.size());
        for (int i = 0; i < limit; i++)
            keys.add(entries.get(i).getKey());
        return keys;
    }

    /** Which metric a top-N selection ranks statements by (see {@link #collectTopStatementKeys}). */
    private enum MetricRank { TOTAL, AVG, MAX }

    /** Average nanos per execution — the ranking value for {@link MetricRank#AVG}. */
    private static double avgNanos(StatementStat s) {
        return s.count > 0 ? (double) s.totalNanos / s.count : 0;
    }

    /**
     * Adds the keys of the top-{@code n} statements of the given {@code kind}, ranked by {@code metric}
     * (descending), to {@code out} — considering only statements with at least {@code minCount} runs.
     * A dedup set on the caller's side means overlapping rankings (a query that leads by both total and
     * max) contribute their key just once.
     */
    private void collectTopStatementKeys(Set<String> out, Kind kind, int n, MetricRank metric, long minCount) {
        List<Map.Entry<String, StatementStat>> entries = new ArrayList<>();
        for (Map.Entry<String, StatementStat> e : statementStats.entrySet()) {
            StatementStat s = e.getValue();
            if (s.kind == kind && s.count >= minCount)
                entries.add(e);
        }
        Comparator<Map.Entry<String, StatementStat>> desc;
        switch (metric) {
            case AVG:
                desc = (a, b) -> Double.compare(avgNanos(b.getValue()), avgNanos(a.getValue()));
                break;
            case MAX:
                desc = (a, b) -> Long.compare(b.getValue().maxNanos, a.getValue().maxNanos);
                break;
            case TOTAL:
            default:
                desc = (a, b) -> Long.compare(b.getValue().totalNanos, a.getValue().totalNanos);
                break;
        }
        entries.sort(desc);
        int limit = Math.min(Math.max(0, n), entries.size());
        for (int i = 0; i < limit; i++)
            out.add(entries.get(i).getKey());
    }

    /** Immutable snapshot with the default top-statements count. */
    public Snapshot snapshot() {
        return snapshot(DEFAULT_TOP_STATEMENTS);
    }

    /**
     * Immutable snapshot of both kinds, plus the top-N statements by total time and the
     * currently in-flight queries. Queue depths are read live from the AsyncQueues.
     */
    public Snapshot snapshot(int topStatements) {
        // Counters + registry read under this monitor's lock; queue depths read via the queues'
        // own locks (outside this lock — no reverse lock ordering, but keeps the scope minimal).
        long rc, rt, re, wc, wt, we;
        AsyncQueue rq, wq;
        List<StatementSnapshot> statements;
        List<InFlightSnapshot> flights;
        List<ErrorSnapshot> errors;
        synchronized (this) {
            rc = read.count;  rt = read.totalNanos;  re = read.errorCount;  rq = read.queue;
            wc = write.count; wt = write.totalNanos; we = write.errorCount; wq = write.queue;
            long nowNanos = System.nanoTime();
            flights = new ArrayList<>(inFlight.size());
            for (InFlight f : inFlight.values()) {
                flights.add(new InFlightSnapshot(f.id, f.kind, f.statement, (nowNanos - f.startNanos) / 1_000_000L, originOf(f.backoffice)));
            }
            // Statements worth surfacing: for EACH kind, the union of the top-N by total time (where DB
            // time goes), by average time (chronically slow), and by max time (worst single run). The
            // client's "rank by" toggle then presents the true top-N for whichever metric it shows — a
            // query that leads by avg is usually far down the by-total list, so a single by-total ranking
            // (as before) hid the slow-but-infrequent queries an admin is hunting. Avg/max are ranked
            // only among statements with at least MIN_RANKED_COUNT runs (a lone cold outlier isn't a
            // chronic problem). LinkedHashSet dedups while keeping a stable order.
            Set<String> selectedKeys = new LinkedHashSet<>();
            for (Kind kind : Kind.values()) {
                collectTopStatementKeys(selectedKeys, kind, topStatements, MetricRank.TOTAL, 1);
                collectTopStatementKeys(selectedKeys, kind, topStatements, MetricRank.AVG, MIN_RANKED_COUNT);
                collectTopStatementKeys(selectedKeys, kind, topStatements, MetricRank.MAX, MIN_RANKED_COUNT);
            }
            statements = new ArrayList<>(selectedKeys.size());
            for (String key : selectedKeys) {
                StatementStat s = statementStats.get(key);
                statements.add(new StatementSnapshot(key, s.kind, s.count, s.totalNanos, s.maxNanos, originOf(s.sawBackoffice, s.sawFrontoffice)));
            }
            // Recent errors newest-first (descending over the FIFO), so the drill-down leads with the latest.
            errors = new ArrayList<>(recentErrors.size());
            for (Iterator<ErrorEntry> it = recentErrors.descendingIterator(); it.hasNext(); ) {
                ErrorEntry e = it.next();
                errors.add(new ErrorSnapshot(e.epochMillis, e.kind, e.statement, e.message, originOf(e.backoffice)));
            }
        }
        return new Snapshot(kindSnapshot(rc, rt, re, rq), kindSnapshot(wc, wt, we, wq), statements, flights, errors);
    }

    private static KindSnapshot kindSnapshot(long count, long totalNanos, long errorCount, AsyncQueue q) {
        return new KindSnapshot(
            count, totalNanos, errorCount,
            q == null ? 0 : q.getWaitingCount(),
            q == null ? 0 : q.getExecutingCount(),
            q == null ? 0 : q.getExecutingQueueMaxSize(),
            q == null ? 0 : q.getPeakWaitingCount(),
            q == null ? 0 : q.getShedCount());
    }

    private static final class KindCounters {
        private long count;
        private long totalNanos;
        private long errorCount;
        private AsyncQueue queue;
    }

    /** A currently-executing SQL statement, holding the opaque cancel handle (server-only use). */
    private static final class InFlight {
        private final long id;
        private final Kind kind;
        private final String statement;
        private final long startNanos;
        private final Object cancelHandle;
        private final Boolean backoffice; // caller origin: TRUE=BO, FALSE=FO, null=unknown (server-internal)

        InFlight(long id, Kind kind, String statement, long startNanos, Object cancelHandle, Boolean backoffice) {
            this.id = id;
            this.kind = kind;
            this.statement = statement;
            this.startNanos = startNanos;
            this.cancelHandle = cancelHandle;
            this.backoffice = backoffice;
        }
    }

    /** One recorded SQL failure kept in the recent-errors ring buffer (server-internal). */
    private static final class ErrorEntry {
        private final long epochMillis;   // wall-clock time of the failure
        private final Kind kind;
        private final String statement;
        private final String message;     // already truncated
        private final Boolean backoffice; // caller origin: TRUE=BO, FALSE=FO, null=unknown

        ErrorEntry(long epochMillis, Kind kind, String statement, String message, Boolean backoffice) {
            this.epochMillis = epochMillis;
            this.kind = kind;
            this.statement = statement;
            this.message = message;
            this.backoffice = backoffice;
        }
    }

    /** Mutable per-statement rollup. */
    private static final class StatementStat {
        private final Kind kind;
        private long count;
        private long totalNanos;
        private long maxNanos;
        private boolean sawBackoffice; // this statement was run by a back-office caller at least once
        private boolean sawFrontoffice; // …and/or by a front-office caller (both → "both")

        StatementStat(Kind kind) {
            this.kind = kind;
        }
    }

    /** Origin label from accumulated BO/FO flags: "both" / "bo" / "fo" / null (neither seen). */
    private static String originOf(boolean bo, boolean fo) {
        if (bo && fo)
            return "both";
        if (bo)
            return "bo";
        if (fo)
            return "fo";
        return null;
    }

    /** Origin label for a single caller: TRUE → "bo", FALSE → "fo", null → null (unknown). */
    private static String originOf(Boolean backoffice) {
        return backoffice == null ? null : backoffice ? "bo" : "fo";
    }

    /**
     * Per-kind snapshot: cumulative {@code count}/{@code totalNanos}/{@code errorCount} plus the
     * live queue gauges ({@code waiting}, {@code executing}, {@code maxConcurrency} = pool size,
     * {@code peakWaiting} high-water mark). Rates are derived client-side from poll-to-poll deltas.
     */
    public record KindSnapshot(long count, long totalNanos, long errorCount,
                               int waiting, int executing, int maxConcurrency, int peakWaiting, int shed) {}

    /**
     * Per-statement snapshot (ranked by total time). {@code statement} is already parameter-free
     * ($1, $2 …), so it is the natural rollup key.
     */
    public record StatementSnapshot(String statement, Kind kind, long count, long totalNanos, long maxNanos, String origin) {}

    /** A currently-executing SQL statement, with how long it has been running. {@code origin} = bo/fo/null. */
    public record InFlightSnapshot(long id, Kind kind, String statement, long ageMillis, String origin) {}

    /** One recent SQL failure for the "Errors" drill-down. {@code origin} = bo/fo/null; newest first in the snapshot. */
    public record ErrorSnapshot(long epochMillis, Kind kind, String statement, String message, String origin) {}

    /** Immutable snapshot of read/write execution, the top statements, the in-flight queries, and recent errors. */
    public record Snapshot(KindSnapshot read, KindSnapshot write,
                           List<StatementSnapshot> topStatements, List<InFlightSnapshot> inFlight,
                           List<ErrorSnapshot> recentErrors) {}
}
