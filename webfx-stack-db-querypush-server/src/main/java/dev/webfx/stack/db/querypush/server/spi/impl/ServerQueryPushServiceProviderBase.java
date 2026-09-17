package dev.webfx.stack.db.querypush.server.spi.impl;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.db.datascope.DataScope;
import dev.webfx.stack.db.query.CompressionMetrics;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryService;
import dev.webfx.stack.db.query.SqlAnalyzeRegistry;
import dev.webfx.stack.db.query.SqlExecutionMonitor;
import dev.webfx.platform.util.Numbers;
import dev.webfx.platform.boot.ApplicationJobFailures;
import dev.webfx.stack.db.querypush.ActiveDbQueryInfo;
import dev.webfx.stack.db.querypush.BootJobFailureMonitorInfo;
import dev.webfx.stack.db.querypush.CompressionMonitorInfo;
import dev.webfx.stack.db.querypush.DatabaseHealthMonitorInfo;
import dev.webfx.stack.db.querypush.DbErrorMonitorInfo;
import dev.webfx.stack.db.querypush.InFlightQueryMonitorInfo;
import dev.webfx.stack.db.querypush.PulseArgument;
import dev.webfx.stack.db.querypush.QueryPushArgument;
import dev.webfx.stack.db.querypush.NameCountInfo;
import dev.webfx.stack.db.querypush.QueryPushMonitorInfo;
import dev.webfx.stack.db.querypush.QueryPushResult;
import dev.webfx.stack.db.querypush.QueryStreamMonitorInfo;
import dev.webfx.stack.db.querypush.SqlAnalyzeResultInfo;
import dev.webfx.stack.db.querypush.SqlExecutionMonitorInfo;
import dev.webfx.stack.db.querypush.SqlKindMonitorInfo;
import dev.webfx.stack.db.querypush.StatementMonitorInfo;
import dev.webfx.stack.db.querypush.SystemResourceMonitorInfo;
import dev.webfx.stack.db.querypush.diff.QueryResultComparator;
import dev.webfx.stack.db.querypush.diff.QueryResultDiff;
import dev.webfx.stack.db.querypush.server.QueryPushServerService;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.db.querypush.spi.QueryPushServiceProvider;
import dev.webfx.stack.push.server.PushClientMetadata;
import dev.webfx.stack.push.server.PushServerService;
import dev.webfx.stack.session.state.LogoutUserId;
import dev.webfx.stack.session.state.RestrictedPrincipalRegistry;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Bruno Salmon
 */
public abstract class ServerQueryPushServiceProviderBase implements QueryPushServiceProvider {
    private PulsePass pulsePass;

    protected ServerQueryPushServiceProviderBase() {
        PushServerService.addUnresponsivePushClientListener(this::removePushClientStreams);
    }

    @Override
    public Future<Object> executeQueryPush(QueryPushArgument argument) {
        // Dispatching to the correct method in dependence of the argument:
        if (argument.isOpenStreamArgument())
            return openStream(argument);
        if (argument.isUpdateStreamArgument())
            return updateStream(argument);
        if (argument.isCloseStreamArgument())
            return closeStream(argument);
        return Future.failedFuture(new IllegalArgumentException());
    }

    protected abstract Future<Object> openStream(QueryPushArgument argument);

    protected abstract Future<Object> updateStream(QueryPushArgument argument);

    protected abstract Future<Object> closeStream(QueryPushArgument argument);

    @Override
    public void executePulse(PulseArgument argument) {
        synchronized (this) {
            if (pulsePass == null || pulsePass.isFinished()) {
                //Logger.log("Starting new pulse pass");
                pulsePass = createPulsePass(argument);
            } else {
                //Logger.log("Updating pulse pass");
                pulsePass.applyPulseArgument(argument);
            }
        }
    }

    protected abstract PulsePass createPulsePass(PulseArgument argument);

    protected abstract void setStreamQueryArgument(StreamInfo streamInfo, QueryArgument queryArgument);

    protected abstract void removeStream(StreamInfo streamInfo);

    protected abstract void removePushClientStreams(Object clientRunId);

    /** Returns a snapshot of all registered QueryInfos (one per distinct QueryArgument) for monitoring. */
    protected abstract Collection<QueryInfo> getQueryInfos();

    /** Bounds the parameters display string shipped in the monitor snapshot. */
    private static final int MONITOR_PARAMETERS_MAX_LENGTH = 200;

    @Override
    public QueryPushMonitorInfo getMonitorInfo() {
        // This snapshot exposes the push query statements, so it's reserved for logged-in callers
        // (the buscall endpoint turns a null return into a failed future). The React back-office
        // route is additionally super-admin gated on the client. We intentionally gate on a
        // logged-in userId rather than the back-office flag: userId is reliably present in a
        // buscall's thread-local state (same read the auth endpoints rely on), whereas the
        // back-office flag is not consistently propagated on this path.
        Object callerUserId = ThreadLocalStateHolder.getUserId();
        if (LogoutUserId.isLogoutUserIdOrNull(callerUserId)) {
            Console.log("[Monitor] getMonitorInfo denied — no logged-in caller (backoffice="
                + ThreadLocalStateHolder.isBackoffice() + ", userId=" + callerUserId + ")");
            return null;
        }
        List<QueryStreamMonitorInfo> queryStreams = new ArrayList<>();
        for (QueryInfo queryInfo : getQueryInfos()) {
            synchronized (queryInfo) { // same lock as addStreamInfo/removeStreamInfo/pushResultToRelevantClients
                Object[] queryStreamIds = new Object[queryInfo.streamInfos.size()];
                Set<Object> clientRunIds = new HashSet<>();
                Set<Object> userIds = new HashSet<>();
                boolean sawBackoffice = false, sawFrontoffice = false; // aggregate the subscribers' origin
                for (int i = 0; i < queryStreamIds.length; i++) {
                    StreamInfo streamInfo = queryInfo.streamInfos.get(i);
                    queryStreamIds[i] = streamInfo.queryStreamId;
                    clientRunIds.add(streamInfo.clientRunId);
                    if (!LogoutUserId.isLogoutUserIdOrNull(streamInfo.userId))
                        userIds.add(streamInfo.userId);
                    if (streamInfo.backoffice != null) {
                        if (streamInfo.backoffice)
                            sawBackoffice = true;
                        else
                            sawFrontoffice = true;
                    }
                }
                Object[] parameters = queryInfo.queryArgument.getParameters();
                // This snapshot is served to any signed-in caller, so it must not carry the values:
                // it would hand one account the search terms of every other. A count is enough to see
                // that a stream is parameterised, which is what the monitor is for. The truncation
                // below is kept for the case where a count string is somehow long, and because
                // removing a guard is not this change's job.
                String parametersDisplay = parameters == null || parameters.length == 0 ? null : QueryArgument.describeParameters(parameters);
                if (parametersDisplay != null && parametersDisplay.length() > MONITOR_PARAMETERS_MAX_LENGTH)
                    parametersDisplay = parametersDisplay.substring(0, MONITOR_PARAMETERS_MAX_LENGTH) + "…";
                queryStreams.add(new QueryStreamMonitorInfo(
                    queryStreamIds,
                    queryInfo.queryArgument.getStatement(),
                    parametersDisplay,
                    queryInfo.lastQueryResult == null ? -1 : queryInfo.lastQueryResult.getRowCount(),
                    queryInfo.streamInfos.size(),
                    queryInfo.activeStreamCount,
                    clientRunIds.size(),
                    userIds.size(),
                    queryInfo.lastQueryExecutionTime == 0 ? -1 : now() - queryInfo.lastQueryExecutionTime,
                    originOf(sawBackoffice, sawFrontoffice)));
            }
        }
        // Snapshot the connected clients ONCE (a defensive copy) and derive the breakdowns + the
        // signed-in count from it — a single, consistent view, and one pass over the push registry.
        List<PushClientMetadata> connectedClients = PushServerService.snapshotConnectedClients();
        // Distinct signed-in users among CONNECTED clients (not subscription-gated): each connected
        // client carries its session's current userId; count the distinct non-logged-out ones.
        Set<Object> signedInUsers = new HashSet<>();
        for (PushClientMetadata c : connectedClients)
            if (!LogoutUserId.isLogoutUserIdOrNull(c.getUserId()))
                signedInUsers.add(c.getUserId());
        return new QueryPushMonitorInfo(
            PushServerService.getPushClientsCount(),
            signedInUsers.size(),
            queryStreams.toArray(new QueryStreamMonitorInfo[0]),
            buildSqlExecutionInfo(),
            buildCompressionInfo(),
            buildClientVersionDistribution(connectedClients),
            buildClientPwaDistribution(connectedClients),
            buildProfileDistribution(connectedClients, PROFILE_BROWSER),
            buildProfileDistribution(connectedClients, PROFILE_OS),
            buildProfileDistribution(connectedClients, PROFILE_DEVICE),
            buildSignInStatusDistribution(connectedClients),
            buildClientAppDistribution(connectedClients),
            buildSystemResourceInfo(),
            buildBootFailures());
    }

    /**
     * Maps this task's retained boot-job failures (from the platform boot layer) into wire DTOs for the
     * /monitor "Boot" card. Empty on a clean boot; a non-empty list means one or more application jobs
     * threw during init/start and the task is only partially functional. Per server process, so it
     * reflects the exact task the page is connected to.
     */
    private static BootJobFailureMonitorInfo[] buildBootFailures() {
        List<ApplicationJobFailures.Failure> fs = ApplicationJobFailures.snapshot();
        BootJobFailureMonitorInfo[] failures = new BootJobFailureMonitorInfo[fs.size()];
        for (int i = 0; i < failures.length; i++) {
            ApplicationJobFailures.Failure f = fs.get(i);
            failures[i] = new BootJobFailureMonitorInfo(f.getEpochMillis(), f.getPhase(), f.getJobName(), f.getMessage(), f.getStackTrace());
        }
        return failures;
    }

    /**
     * JVM CPU + heap-memory snapshot for the /monitor page, from the standard management beans.
     * Defensive: any bean unavailability is swallowed to a safe default (never fails getMonitorInfo).
     */
    private static SystemResourceMonitorInfo buildSystemResourceInfo() {
        // CPU: fraction of CPU used by THIS JVM process, normalised by the (container-aware) processor
        // count. -1 (or NaN) means "not yet available" — the very first sample after start.
        double cpuLoad = -1;
        try {
            OperatingSystemMXBean os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            if (os != null) {
                cpuLoad = os.getProcessCpuLoad();
                if (Double.isNaN(cpuLoad))
                    cpuLoad = -1;
            }
        } catch (Throwable ignored) { /* keep -1 */ }
        int processors = Runtime.getRuntime().availableProcessors();

        // Heap: used / committed / max (max reflects -Xmx or the default MaxRAMPercentage of the cgroup limit).
        long heapUsed = 0, heapCommitted = 0, heapMax = -1;
        try {
            MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            heapUsed = heap.getUsed();
            heapCommitted = heap.getCommitted();
            heapMax = heap.getMax();
        } catch (Throwable ignored) { /* keep defaults */ }

        // Retained data signal: old-gen occupancy AFTER the last collection (live set, sawtooth removed).
        long oldGenAfterGc = -1;
        try {
            for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                if (pool.getType() == MemoryType.HEAP && isOldGenPool(pool.getName())) {
                    MemoryUsage afterGc = pool.getCollectionUsage(); // null if the pool doesn't track it
                    if (afterGc != null)
                        oldGenAfterGc = afterGc.getUsed();
                }
            }
        } catch (Throwable ignored) { /* keep -1 */ }

        // GC pressure: cumulative collection count + time across all collectors (client derives a rate).
        long gcCount = 0, gcTimeMillis = 0;
        try {
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
                long c = gc.getCollectionCount();
                if (c > 0)
                    gcCount += c;
                long t = gc.getCollectionTime();
                if (t > 0)
                    gcTimeMillis += t;
            }
        } catch (Throwable ignored) { /* keep 0 */ }

        // Uptime — a resetting value across polls flags crashes / OOM-kills (the server restarted).
        long uptimeMillis = 0;
        try {
            uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        } catch (Throwable ignored) { /* keep 0 */ }

        // Event-loop lag: start the probe (once) and take the worst lag since the previous poll.
        ensureEventLoopProbe();
        long eventLoopLagMillis = takeEventLoopLagMillis();

        return new SystemResourceMonitorInfo(cpuLoad, processors, heapUsed, heapCommitted, heapMax,
            oldGenAfterGc, gcCount, gcTimeMillis, uptimeMillis, eventLoopLagMillis, SERVER_VERSION);
    }

    /**
     * Server build version — the shared APP_BUILD_TIMESTAMP baked into this deploy (same string as the
     * React clients built in the same CI workflow), read once from the container env. Null when unset
     * (e.g. a local dev run), so the /monitor Uptime card just omits the version there.
     */
    private static final String SERVER_VERSION = readServerVersion();

    private static String readServerVersion() {
        try {
            String v = System.getenv("APP_BUILD_TIMESTAMP");
            return v == null || v.isEmpty() ? null : v;
        } catch (Throwable t) {
            return null;
        }
    }

    // ---- Vert.x event-loop lag probe ----------------------------------------------------------------
    // A blocked event loop stalls EVERY client while CPU can still look idle — the signature Vert.x
    // health signal. We schedule a periodic no-op on an event loop (webfx Scheduler → vertx.setPeriodic)
    // and measure how late each tick fires vs its interval; that scheduling delay IS the loop's lag.
    // Only measures the one event loop the timer lands on — a reasonable single-instance proxy.

    private static final long EVENT_LOOP_PROBE_INTERVAL_MS = 250;
    private static final AtomicBoolean EVENT_LOOP_PROBE_STARTED = new AtomicBoolean(false);
    /** Worst lag (ms) seen since the last {@link #takeEventLoopLagMillis()}; -1 means "no sample yet". */
    private static final AtomicLong EVENT_LOOP_LAG_MAX_MILLIS = new AtomicLong(-1);
    /** Previous tick time (ns). Touched only on the probe's own event-loop thread. */
    private static long eventLoopLastProbeNanos;

    /** Starts the periodic event-loop lag probe once (lazily, on first /monitor view). */
    private static void ensureEventLoopProbe() {
        if (EVENT_LOOP_PROBE_STARTED.compareAndSet(false, true)) {
            eventLoopLastProbeNanos = System.nanoTime();
            Scheduler.schedulePeriodic(EVENT_LOOP_PROBE_INTERVAL_MS, () -> {
                long now = System.nanoTime();
                long elapsedMs = (now - eventLoopLastProbeNanos) / 1_000_000;
                eventLoopLastProbeNanos = now;
                long lag = elapsedMs - EVENT_LOOP_PROBE_INTERVAL_MS; // how much later than scheduled this tick fired
                long clamped = Math.max(0, lag);
                EVENT_LOOP_LAG_MAX_MILLIS.updateAndGet(prev -> Math.max(prev, clamped));
            });
        }
    }

    /** Worst event-loop lag since the previous call, then resets; -1 if the probe hasn't ticked yet. */
    private static long takeEventLoopLagMillis() {
        return EVENT_LOOP_LAG_MAX_MILLIS.getAndSet(-1);
    }

    // ---- Database health (on-demand; queries pg_stat_activity, kept off the regular monitor poll) ----

    // ONE statement, not two. The counts and the query list used to be two statements, and
    // QueryService runs a batch through executeParallel — two pooled connections, each seeing the
    // other as an active backend. The card then read "2 active" while the list, excluding only its
    // own pid, showed a single row: this monitoring query. Asking once fixes that at the root — one
    // backend to discount (pg_backend_pid()), and one sample, so the counts and the rows can never
    // disagree about the same instant. The two halves share a `snapshot` CTE, referenced twice and
    // therefore materialised, so even within this statement pg_stat_activity is read once; it is a
    // shared-memory view, not an MVCC table, so a second scan could otherwise see a later reality.
    //
    // Shape: the counts are one row, the busy backends are 0..50 rows, and a LEFT JOIN ON TRUE pairs
    // them — every row carries the same counts, and an idle database still yields one row with the
    // query columns NULL. See the column indices below for how it is read back.
    //
    // What is deliberately NOT excluded: another server task's or another admin's health query. That
    // is somebody else's work on this database, and it stays visible in both the counts and the list.
    private static final String DB_HEALTH_SQL =
        "with snapshot as (" +
        "  select pid, backend_type, state, query, query_start, wait_event_type from pg_stat_activity" +
        ")," +
        // Connection counts + max_connections in one row. Counts only CLIENT backends — Postgres's own
        // background/auxiliary processes (autovacuum launcher, bgwriter, checkpointer, walwriter, …)
        // have a NULL state and don't count against max_connections, so excluding them makes total
        // meaningful (vs max_connections) and lets the client sum active + idle + other +
        // not_inspectable + self = total. "other" (client backends in states like "idle in
        // transaction") is the one bucket with no column of its own: the client derives it by
        // subtracting the other four from total.
        " counts as (" +
        "  select (select setting::int from pg_settings where name='max_connections') as max_conn," +
        "   count(*) filter (where backend_type = 'client backend')::int as total," +
        // "active" means real CLIENT work, so this snapshot's own backend — active only because we
        // asked — is counted apart (self, below) rather than making an idle database read as busy.
        "   count(*) filter (where backend_type = 'client backend' and state = 'active' and pid <> pg_backend_pid())::int as active," +
        "   count(*) filter (where backend_type = 'client backend' and state = 'idle')::int as idle," +
        // Client backends whose state reads NULL: Postgres blanks state/query/query_start for
        // sessions belonging to roles this one cannot inspect (not a superuser, not a member of
        // pg_read_all_stats / pg_monitor). Those are exactly the rows the busy list drops on
        // "state is not null", so without this the drill-down silently shows only our OWN backends
        // while claiming to cover all clients. Counting them turns an invisible blind spot into a
        // visible one: a non-zero value means "there are N backends here whose queries I cannot see".
        "   count(*) filter (where backend_type = 'client backend' and state is null)::int as not_inspectable," +
        // This snapshot's own connection — the one row the busy list drops. Counted rather than
        // silently subtracted, so the client's derived "other" bucket stays what it claims to be
        // (idle-in-transaction and friends) and active + idle + other + not_inspectable + self still
        // equals total. Left IN total on purpose: it is a real connection holding a real slot against
        // max_connections, which is what the card's total measures.
        "   count(*) filter (where backend_type = 'client backend' and pid = pg_backend_pid())::int as self" +
        "  from snapshot" +
        ")," +
        // Non-idle backends (long-running / blocking) across all clients, worst-first, capped.
        " busy as (" +
        "  select pid," +
        "   (extract(epoch from (now() - query_start)) * 1000)::bigint as duration_ms," +
        "   state," +
        "   wait_event_type as wait," +
        "   coalesce((pg_blocking_pids(pid))[1], 0) as blocked_by," +
        "   left(query, 500) as query" +
        "  from snapshot" +
        "  where state is not null and state <> 'idle' and pid <> pg_backend_pid()" +
        "  order by duration_ms desc nulls last" +
        "  limit 50" +
        ")" +
        " select counts.max_conn, counts.total, counts.active, counts.idle, counts.not_inspectable, counts.self," +
        "  busy.pid, busy.duration_ms, busy.state, busy.wait, busy.blocked_by, busy.query" +
        " from counts left join busy on true" +
        " order by busy.duration_ms desc nulls last";

    @Override
    public Future<DatabaseHealthMonitorInfo> getDatabaseHealthInfo() {
        // Same gate as getMonitorInfo — a read-only DB snapshot reserved for logged-in back-office callers.
        Object callerUserId = ThreadLocalStateHolder.getUserId();
        if (LogoutUserId.isLogoutUserIdOrNull(callerUserId)) {
            Console.log("[Monitor] getDatabaseHealthInfo denied — no logged-in caller (userId=" + callerUserId + ")");
            return Future.failedFuture(new IllegalStateException("Database health info is not available"));
        }
        Object dataSourceId = DataSourceModelService.getDefaultDataSourceId();
        return QueryService.executeQuery(rawSqlQuery(dataSourceId, DB_HEALTH_SQL))
            .map(ServerQueryPushServiceProviderBase::buildDatabaseHealthInfo);
    }

    /** A raw-SQL read query: language is null (not "DQL") so it bypasses DQL compilation; sendMetadata=true so columns come back by name. */
    private static QueryArgument rawSqlQuery(Object dataSourceId, String sql) {
        return new QueryArgument(null, dataSourceId, null, null, sql, null, null, true, false);
    }

    // Column indices, in SELECT order — a raw-SQL QueryResult carries values but no columnNames, so
    // we read by position, not by name. Every row repeats the counts; the query columns are NULL on
    // the single row an idle database returns (the LEFT JOIN's unmatched side).
    private static final int H_MAX = 0, H_TOTAL = 1, H_ACTIVE = 2, H_IDLE = 3, H_NOT_INSPECTABLE = 4, H_SELF = 5,
        H_PID = 6, H_DURATION = 7, H_STATE = 8, H_WAIT = 9, H_BLOCKED_BY = 10, H_QUERY = 11;

    private static DatabaseHealthMonitorInfo buildDatabaseHealthInfo(QueryResult result) {
        int rowCount = result == null ? 0 : result.getRowCount();
        if (rowCount == 0) // the LEFT JOIN always yields at least one row, so this means the query failed to shape
            return new DatabaseHealthMonitorInfo(-1, 0, 0, 0, 0, 0, new ActiveDbQueryInfo[0]);
        List<ActiveDbQueryInfo> queries = new ArrayList<>(rowCount);
        for (int r = 0; r < rowCount; r++) {
            if (result.getValue(r, H_PID) == null) // no busy backend on this row: counts only
                continue;
            queries.add(new ActiveDbQueryInfo(
                result.getInt(r, H_PID, 0),
                Numbers.longValue(result.getValue(r, H_DURATION)),
                stringOf(result.getValue(r, H_STATE)),
                stringOf(result.getValue(r, H_WAIT)),
                result.getInt(r, H_BLOCKED_BY, 0),
                stringOf(result.getValue(r, H_QUERY))));
        }
        return new DatabaseHealthMonitorInfo(
            result.getInt(0, H_MAX, -1),
            result.getInt(0, H_TOTAL, 0),
            result.getInt(0, H_ACTIVE, 0),
            result.getInt(0, H_IDLE, 0),
            result.getInt(0, H_NOT_INSPECTABLE, 0),
            result.getInt(0, H_SELF, 0),
            queries.toArray(new ActiveDbQueryInfo[0]));
    }

    /** Null-safe toString for a raw QueryResult cell (text columns come back as String, but be defensive). */
    private static String stringOf(Object value) {
        return value == null ? null : value.toString();
    }

    /** Whether a memory-pool name denotes the tenured/old generation (collector-independent). */
    private static boolean isOldGenPool(String poolName) {
        return poolName != null && (poolName.contains("Old") || poolName.contains("Tenured"));
    }

    /** Placeholder bucket name for a connected client that hasn't reported the fact yet (older client). */
    private static final String UNKNOWN_BUCKET = "unknown";

    /** Sign-in status bucket for a client with no signed-in user (logged out / never signed in). */
    private static final String SIGN_IN_ANONYMOUS = "anonymous";

    // Slot indexes in a client's compact "browser|os|deviceType" profile string (see clientDeviceProfile).
    private static final int PROFILE_BROWSER = 0;
    private static final int PROFILE_OS = 1;
    private static final int PROFILE_DEVICE = 2;

    /** Connected-clients breakdown by build version (an "unknown" bucket for clients that don't report it). */
    private static NameCountInfo[] buildClientVersionDistribution(List<PushClientMetadata> connectedClients) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PushClientMetadata c : connectedClients) {
            String version = c.getClientVersion();
            counts.merge(version == null ? UNKNOWN_BUCKET : version, 1, Integer::sum);
        }
        return toNameCounts(counts);
    }

    /** Connected-clients breakdown by PWA display mode: installed / browser / unknown. */
    private static NameCountInfo[] buildClientPwaDistribution(List<PushClientMetadata> connectedClients) {
        // LinkedHashMap keeps a stable, meaningful order (installed, browser, unknown) for display.
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("installed", 0);
        counts.put("browser", 0);
        int unknown = 0;
        for (PushClientMetadata c : connectedClients) {
            Boolean pwa = c.getPwa();
            if (pwa == null)
                unknown++;
            else
                counts.merge(pwa ? "installed" : "browser", 1, Integer::sum);
        }
        if (unknown > 0)
            counts.put(UNKNOWN_BUCKET, unknown);
        return toNameCounts(counts);
    }

    /**
     * Connected-clients breakdown by one slot of the compact "browser|os|deviceType" profile
     * (PROFILE_BROWSER / PROFILE_OS / PROFILE_DEVICE). A client that didn't report a profile — or
     * reported "unknown" for that slot — falls into the "unknown" bucket (both map to UNKNOWN_BUCKET).
     */
    private static NameCountInfo[] buildProfileDistribution(List<PushClientMetadata> connectedClients, int slot) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PushClientMetadata c : connectedClients)
            counts.merge(profileSlot(c.getClientProfile(), slot), 1, Integer::sum);
        return toNameCounts(counts);
    }

    /** Reads one "|"-separated slot of a client profile, mapping a null/short/empty value to "unknown". */
    private static String profileSlot(String clientProfile, int slot) {
        if (clientProfile == null || clientProfile.isEmpty())
            return UNKNOWN_BUCKET;
        String[] parts = clientProfile.split("\\|", -1);
        if (slot >= parts.length)
            return UNKNOWN_BUCKET;
        String value = parts[slot].trim();
        return value.isEmpty() ? UNKNOWN_BUCKET : value;
    }

    /**
     * Connected-clients breakdown by sign-in status — a per-connection partition of the connected
     * clients (a signed-in user with two tabs counts as two). Buckets: "anonymous" (logged out / never
     * signed in) or the userId principal's type name (e.g. "ModalityUserPrincipal" /
     * "ModalityGuestPrincipal"). Kept modality-agnostic: the framework forwards the principal's type
     * name and the back-office maps it to a human label, so no application type leaks in here.
     */
    private static NameCountInfo[] buildSignInStatusDistribution(List<PushClientMetadata> connectedClients) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PushClientMetadata c : connectedClients)
            counts.merge(signInStatusOf(c.getUserId()), 1, Integer::sum);
        return toNameCounts(counts);
    }

    /** "anonymous" for a logged-out/absent user, else the userId principal's simple type name. */
    private static String signInStatusOf(Object userId) {
        return LogoutUserId.isLogoutUserIdOrNull(userId) ? SIGN_IN_ANONYMOUS : userId.getClass().getSimpleName();
    }

    /**
     * Connected-clients breakdown by app: "bo" (back-office) vs "fo" (front-office). Only a back-office
     * build sets the backoffice flag to TRUE; a front-office build leaves it null (it never sends
     * false), so anything non-TRUE is front-office — the same reading as {@code ThreadLocalStateHolder
     * .isBackoffice()} used elsewhere. Every connected client is one or the other, so there's no
     * "unknown" bucket (a client that hasn't reported the flag yet is counted as front-office).
     */
    private static NameCountInfo[] buildClientAppDistribution(List<PushClientMetadata> connectedClients) {
        // LinkedHashMap keeps a stable, meaningful order (bo, fo) for display.
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("bo", 0);
        counts.put("fo", 0);
        for (PushClientMetadata c : connectedClients)
            counts.merge(Boolean.TRUE.equals(c.getBackoffice()) ? "bo" : "fo", 1, Integer::sum);
        return toNameCounts(counts);
    }

    private static NameCountInfo[] toNameCounts(Map<String, Integer> counts) {
        NameCountInfo[] result = new NameCountInfo[counts.size()];
        int i = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet())
            result[i++] = new NameCountInfo(e.getKey(), e.getValue());
        return result;
    }

    /**
     * Whether the caller on this thread is a read-only session (e.g. a support member borrowing
     * another user's view). The monitor MUTATORS below refuse such callers: cancelling queries,
     * arming EXPLAIN ANALYZE or resetting counters are state changes that never pass the SQL submit
     * layer where read-only is otherwise enforced, so each mutator has to refuse for itself. The
     * read-only snapshots (getMonitorInfo &amp; co) deliberately keep serving them. Only valid in the
     * synchronous part of a call, which is where all four mutators run in full.
     */
    private static boolean isRestrictedCaller(String mutatorName) {
        if (RestrictedPrincipalRegistry.isCurrentUserRestricted()) {
            Console.log("[Monitor] " + mutatorName + " denied — read-only session");
            return true;
        }
        return false;
    }

    @Override
    public Boolean cancelSqlQuery(long monitorId) {
        // Same gate as getMonitorInfo: cancelling a running query is an admin action reserved for
        // logged-in callers (super-admin gated on the client). A null return makes the buscall
        // endpoint fail rather than silently no-op for an unauthorized caller.
        Object callerUserId = ThreadLocalStateHolder.getUserId();
        if (LogoutUserId.isLogoutUserIdOrNull(callerUserId)) {
            Console.log("[Monitor] cancelSqlQuery denied — no logged-in caller (userId=" + callerUserId + ")");
            return null;
        }
        if (isRestrictedCaller("cancelSqlQuery"))
            return null;
        boolean dispatched = SqlExecutionMonitor.get().cancel(monitorId);
        Console.log("[Monitor] cancelSqlQuery id=" + monitorId + " → "
            + (dispatched ? "cancel dispatched" : "not found / already finished"));
        return dispatched;
    }

    @Override
    public Boolean armSqlAnalyze(String statement) {
        // Same gate as getMonitorInfo. Reserved for logged-in callers; a null return fails the call.
        Object callerUserId = ThreadLocalStateHolder.getUserId();
        if (LogoutUserId.isLogoutUserIdOrNull(callerUserId)) {
            Console.log("[Monitor] armSqlAnalyze denied — no logged-in caller (userId=" + callerUserId + ")");
            return null;
        }
        if (isRestrictedCaller("armSqlAnalyze"))
            return null;
        // Only arm statements the server actually runs as reads — never arbitrary client SQL, and
        // never a write (EXPLAIN ANALYZE would execute it).
        if (!SqlExecutionMonitor.get().isKnownReadStatement(statement)) {
            Console.log("[Monitor] armSqlAnalyze rejected — not a known read statement");
            return false;
        }
        SqlAnalyzeRegistry.get().arm(statement, System.currentTimeMillis());
        Console.log("[Monitor] armSqlAnalyze armed a read statement for next-occurrence EXPLAIN");
        return true;
    }

    @Override
    public SqlAnalyzeResultInfo getSqlAnalyzeResult(String statement) {
        Object callerUserId = ThreadLocalStateHolder.getUserId();
        if (LogoutUserId.isLogoutUserIdOrNull(callerUserId))
            return null;
        long now = System.currentTimeMillis();
        return toAnalyzeInfo(SqlAnalyzeRegistry.get().getResult(statement, now), now);
    }

    @Override
    public Boolean resetSqlAnalyze(String statement) {
        // Same gate as getMonitorInfo. A null return fails the buscall for an unauthorized caller.
        Object callerUserId = ThreadLocalStateHolder.getUserId();
        if (LogoutUserId.isLogoutUserIdOrNull(callerUserId))
            return null;
        if (isRestrictedCaller("resetSqlAnalyze"))
            return null;
        SqlAnalyzeRegistry.get().remove(statement);
        return true;
    }

    @Override
    public Boolean resetSqlMonitor() {
        // Same gate as getMonitorInfo. A null return fails the buscall for an unauthorized caller.
        Object callerUserId = ThreadLocalStateHolder.getUserId();
        if (LogoutUserId.isLogoutUserIdOrNull(callerUserId)) {
            Console.log("[Monitor] resetSqlMonitor denied — no logged-in caller (userId=" + callerUserId + ")");
            return null;
        }
        if (isRestrictedCaller("resetSqlMonitor"))
            return null;
        SqlExecutionMonitor.get().reset();
        CompressionMetrics.reset();
        SqlAnalyzeRegistry.get().clearAll(); // fresh window drops every pending/captured analyze arm
        Console.log("[Monitor] resetSqlMonitor — SQL execution counters + per-statement rollup + compression + analyze arms cleared");
        return true;
    }

    /** Maps a registry result to its wire DTO (carrying the statement so the snapshot list is self-identifying). */
    private static SqlAnalyzeResultInfo toAnalyzeInfo(SqlAnalyzeRegistry.Result r, long now) {
        switch (r.status) {
            case READY:
                return new SqlAnalyzeResultInfo(r.statement, "ready", r.planText, r.dqlStatement, r.parametersDisplay, Math.max(0, now - r.atMillis), r.clientVersion);
            case PENDING:
                return new SqlAnalyzeResultInfo(r.statement, "pending", null, null, null, -1, null);
            default:
                return new SqlAnalyzeResultInfo(r.statement, "none", null, null, null, -1, null);
        }
    }

    /** Builds the read/write SQL execution DTO (with top statements + in-flight) from the monitor. */
    private static SqlExecutionMonitorInfo buildSqlExecutionInfo() {
        SqlExecutionMonitor.Snapshot s = SqlExecutionMonitor.get().snapshot();
        List<SqlExecutionMonitor.StatementSnapshot> ss = s.topStatements();
        StatementMonitorInfo[] statements = new StatementMonitorInfo[ss.size()];
        for (int i = 0; i < statements.length; i++) {
            SqlExecutionMonitor.StatementSnapshot st = ss.get(i);
            statements[i] = new StatementMonitorInfo(st.statement(), kindName(st.kind()), st.count(), st.totalNanos(), st.maxNanos(), st.origin());
        }
        List<SqlExecutionMonitor.InFlightSnapshot> fs = s.inFlight();
        InFlightQueryMonitorInfo[] flights = new InFlightQueryMonitorInfo[fs.size()];
        for (int i = 0; i < flights.length; i++) {
            SqlExecutionMonitor.InFlightSnapshot f = fs.get(i);
            flights[i] = new InFlightQueryMonitorInfo(f.id(), kindName(f.kind()), f.statement(), f.ageMillis(), f.origin());
        }
        // Armed (pending) + captured (ready) analyze entries, so the page's regular snapshot surfaces
        // a plan whenever the query eventually runs — no client-side polling / timeout.
        long now = System.currentTimeMillis();
        List<SqlAnalyzeRegistry.Result> ar = SqlAnalyzeRegistry.get().snapshotAll();
        SqlAnalyzeResultInfo[] analyze = new SqlAnalyzeResultInfo[ar.size()];
        for (int i = 0; i < analyze.length; i++)
            analyze[i] = toAnalyzeInfo(ar.get(i), now);
        List<SqlExecutionMonitor.ErrorSnapshot> es = s.recentErrors();
        DbErrorMonitorInfo[] errors = new DbErrorMonitorInfo[es.size()];
        for (int i = 0; i < errors.length; i++) {
            SqlExecutionMonitor.ErrorSnapshot e = es.get(i);
            errors[i] = new DbErrorMonitorInfo(e.epochMillis(), kindName(e.kind()), e.statement(), e.message(), e.origin());
        }
        return new SqlExecutionMonitorInfo(toKindInfo(s.read()), toKindInfo(s.write()), statements, flights, analyze, errors);
    }

    private static String kindName(SqlExecutionMonitor.Kind k) {
        return k == SqlExecutionMonitor.Kind.WRITE ? "write" : "read";
    }

    /** Origin label from accumulated BO/FO flags: "both" / "bo" / "fo" / null (neither seen). */
    private static String originOf(boolean backoffice, boolean frontoffice) {
        if (backoffice && frontoffice)
            return "both";
        if (backoffice)
            return "bo";
        if (frontoffice)
            return "fo";
        return null;
    }

    private static SqlKindMonitorInfo toKindInfo(SqlExecutionMonitor.KindSnapshot k) {
        return new SqlKindMonitorInfo(k.count(), k.totalNanos(), k.errorCount(),
            k.waiting(), k.executing(), k.maxConcurrency(), k.peakWaiting(), k.shed());
    }

    /** Builds the compression DTO from the process-wide {@link CompressionMetrics}. */
    private static CompressionMonitorInfo buildCompressionInfo() {
        CompressionMetrics.Snapshot c = CompressionMetrics.snapshot();
        return new CompressionMonitorInfo(c.count(), c.totalNanos(), c.maxNanos(), c.slowCount(), c.totalCells());
    }

    public abstract class PulsePass {
        final long startTime = now();
        int executedQueries, changedQueries, pushedToClients, pushedFailed;
        protected QueryInfo nextMostUrgentQueryNotYetRefreshed;
        boolean finished;

        protected PulsePass(PulseArgument argument) {
            applyPulseArgument(argument);
            refreshNextMostUrgentQueryIfAnyAndLoop();
        }

        protected abstract void applyPulseArgument(PulseArgument argument);

        void refreshNextMostUrgentQueryIfAnyAndLoop() {
            QueryInfo nextMostUrgentQuery = getNextMostUrgentQuery();
            if (nextMostUrgentQuery == null)
                markAsFinished();
            else
                refreshQuery(nextMostUrgentQuery)
                    .onComplete(ar -> refreshNextMostUrgentQueryIfAnyAndLoop());
        }

        Future<Void> refreshQuery(QueryInfo queryInfo) {
            Future<QueryResult> resultFuture;
            // Reuse the cached lastQueryResult when:
            //   - the query is clean (no submits since last execute), OR
            //   - the query is dirty BUT we're still inside the throttle
            //     window — this is the reactivated-newcomer path; we push
            //     the cached result to them rather than re-executing SQL
            //     for every burst submit.
            if (queryInfo.lastQueryResult != null
                    && (!queryInfo.isDirty() || queryInfo.isWithinExecuteThrottle())) {
                resultFuture = Future.succeededFuture(queryInfo.lastQueryResult);
            } else { // Otherwise asking the query service to execute the query
                executedQueries++;
                queryInfo.touchExecuted();
                // Re-fires (lastQueryResult != null) drop a priority tier so a user-facing
                // one-shot query or submit can jump ahead of a freshness refresh on the
                // AsyncQueue. Initial fires keep the subscription's original priority so
                // first paint competes fairly with one-shot reads. See PUSH_REFRESH_PRIORITY_DELTA.
                QueryArgument executeArgument = queryInfo.lastQueryResult == null
                    ? queryInfo.queryArgument
                    : withPushRefreshPriority(queryInfo.queryArgument);
                resultFuture = QueryService.executeQuery(executeArgument)
                    .onFailure(Console::error);
            }
            // Calling the pushResultToRelevantClients() method when the result is ready
            return resultFuture.map(queryResult -> {
                pushResultToRelevantClients(queryInfo, queryResult);
                return null;
            });
        }

        void pushResultToRelevantClients(QueryInfo queryInfo, QueryResult queryResult) {
            synchronized (queryInfo) {
                // Retrieving the last result
                QueryResult lastQueryResult = queryInfo.lastQueryResult;
                List<StreamInfo> activeNewClients = Collections.filter(queryInfo.streamInfos, si -> si.isActive() && (lastQueryResult == null || si.lastQueryResult != lastQueryResult));
                List<StreamInfo> activeOldClients = Collections.filter(queryInfo.streamInfos, si -> si.isActive() && lastQueryResult != null && si.lastQueryResult == lastQueryResult);
                queryInfo.activeNewStreamCount = activeNewClients.size();
                // Checking if there are changes compared to the last result - false if there are no "old" clients (that have already received the last result)
                boolean hasChanged = !activeOldClients.isEmpty() && queryInfo.hasQueryResultChanged(queryResult);
                if (hasChanged) // Increasing the changed queries counter in this case
                    changedQueries++;
                // Setting the version number (same if no change or +1 if changed)
                if (lastQueryResult != null)
                    queryResult.setVersionNumber(lastQueryResult.getVersionNumber() + (hasChanged ? 1 : 0));
                // Computing the diff in case of changes
                QueryResultDiff queryResultDiff = hasChanged ? QueryResultComparator.computeDiff(lastQueryResult, queryResult) : null;
                // Sending the whole query result to new clients (the blank streams that haven't received any result yet)
                pushResultToClients(activeNewClients, queryResult, null);
                // Updating queryInfo fields
                if (lastQueryResult == null || hasChanged)
                    queryInfo.lastQueryResult = queryResult;
                queryInfo.reactivated = false;
                // Sending only the diff to old clients (or sending the whole result if the comparator couldn't compute a diff)
                if (hasChanged)
                    pushResultToClients(activeOldClients, queryResult, queryResultDiff);
                else // Otherwise (no diff), marking new clients as old
                    Collections.forEach(activeNewClients, si -> si.lastQueryResult = queryInfo.lastQueryResult);
            }
        }

        void pushResultToClients(Collection<StreamInfo> streamInfos, QueryResult queryResult, QueryResultDiff queryResultDiff) {
/*
            if (queryResultDiff != null)
                Logger.log("Pushing diff " + queryResultDiff.getPreviousQueryResultVersionNumber() + " -> " + queryResultDiff.getFinalQueryResultVersionNumber() + " to " + streamInfos.size() + " clients");
            else
                Logger.log("Pushing result " + queryResult.getVersionNumber() + " to " + streamInfos.size() + " clients");
*/
            for (StreamInfo streamInfo : streamInfos) {
                pushedToClients++;
                pushResultToClient(streamInfo, queryResult, queryResultDiff);
            }
        }

        void pushResultToClient(StreamInfo streamInfo, QueryResult queryResult, QueryResultDiff queryResultDiff) {
            streamInfo.lastQueryResult = queryResult;
            QueryResult wireResult = trimForPush(streamInfo, queryResult);
            Object queryStreamId = streamInfo.queryStreamId;
            Console.log("pushResultToClient() to queryStreamId=" + queryStreamId + " with " + (queryResult != null ? queryResult.getRowCount() + " rows" : "diff"));
            QueryPushServerService.pushQueryResultToClient(new QueryPushResult(queryStreamId, wireResult, queryResultDiff), streamInfo.clientRunId)
                .onFailure(cause -> { // Handling push call failure
                    long timeSinceCreation = now() - streamInfo.creationTime;
                    if (timeSinceCreation < 1_000)
                        Scheduler.scheduleDelay(100, () -> {
                            Console.log("Retrying result push to client " + streamInfo.clientRunId + " since it failed less than 1s after the stream creation (the client push registration may have not been completed)");
                            pushResultToClient(streamInfo, queryResult, queryResultDiff);
                        });
                    else {
                        Console.log("Result push failed :" + cause.getMessage());
                        pushedFailed++;
                        removeStream(streamInfo);
                    }
                });
        }

        QueryInfo getNextMostUrgentQuery() {
            QueryInfo nextMostUrgentQuery = getNextMostUrgentQueryNotYetRefreshed();
            nextMostUrgentQueryNotYetRefreshed = null;
            return nextMostUrgentQuery;
        }

        QueryInfo getNextMostUrgentQueryNotYetRefreshed() {
            if (nextMostUrgentQueryNotYetRefreshed == null)
                nextMostUrgentQueryNotYetRefreshed = fetchNextMostUrgentQuery();
            return nextMostUrgentQueryNotYetRefreshed;
        }

        protected abstract QueryInfo fetchNextMostUrgentQuery();

        public void markAsFinished() {
            finished = true;
            onFinished();
        }

        boolean isFinished() {
            return finished;
        }

        void onFinished() {
            StringBuilder sb = new StringBuilder(finishedStringStart());
            if (executedQueries > 0) {
                sb.append(", changed: ").append(changedQueries);
                if (pushedToClients > 0) {
                    sb.append(", pushed: ").append(pushedToClients);
                    if (pushedFailed > 0)
                        sb.append(", failed: ").append(pushedFailed);
                }
            }
            Console.log(sb);
        }

        protected String finishedStringStart() {
            return "Pulse finished in " + (now() - startTime) + "ms - executed queries: " + executedQueries;
        }
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    /**
     * Returns a copy of the subscription's QueryArgument with priority lowered by
     * {@link QueryArgument#PUSH_REFRESH_PRIORITY_DELTA} — applied only to push re-fires.
     */
    private static QueryArgument withPushRefreshPriority(QueryArgument argument) {
        return QueryArgument.builder()
            .copy(argument)
            .setPriority(argument.getPriority() + QueryArgument.PUSH_REFRESH_PRIORITY_DELTA)
            .build();
    }

    /**
     * Minimum interval between SQL re-executions of the same QueryInfo when
     * it has been dirtied by a submit. Caps the worst-case execution rate
     * regardless of how many submits arrive — without this, a high-frequency
     * data source (e.g. 800 livestream viewers heartbeating once a minute →
     * ~13 MediaConsumption UPDATEs per second) would force back-to-back
     * re-executions of every push query whose data scope intersects
     * MediaConsumption, even though the result barely changes between
     * consecutive submits.
     *
     * Doesn't affect new subscribers: a stream that was just added is
     * `reactivated` and bypasses the throttle so the cached last result is
     * pushed to it immediately.
     */
    private static final long MIN_REFRESH_INTERVAL_MS = 1_000;

    public static final class QueryInfo {
        private final QueryArgument queryArgument;
        private final List<StreamInfo> streamInfos = new ArrayList<>(); // Contains new client streams that haven't received any result yet
        private int activeNewStreamCount;
        /*
                private final List<StreamInfo> blankStreamInfos = new ArrayList<>(); // Contains new client streams that haven't received any result yet
                private final List<StreamInfo> filledStreamInfos = new ArrayList<>(); // Contains client streams that already received at least 1 result
        */
        private int activeStreamCount;
        private long lastQueryExecutionTime;
        private long lastPossibleChangeTime = now();
        private boolean reactivated;
        private QueryResult lastQueryResult;

        public QueryInfo(QueryArgument queryArgument) {
            this.queryArgument = queryArgument;
        }

        public QueryArgument getQueryArgument() {
            return queryArgument;
        }

        void touchExecuted() {
            lastQueryExecutionTime = now();
            lastPossibleChangeTime = 0;
            reactivated = false;
        }

        boolean hasQueryResultChanged(QueryResult newQueryResult) {
            return !Objects.equals(newQueryResult, lastQueryResult);
        }

        public void markAsDirty() {
            lastPossibleChangeTime = now();
        }

        public void markAsReactivated() {
            reactivated = true;
            //Logger.log("Marked " + queryArgument + " as reactivated");
        }

        public boolean needsRefresh() {
            // `reactivated` (new stream just added) bypasses the throttle so
            // the cached lastQueryResult gets pushed to the newcomer at once.
            // For dirty-due-to-submits, hold off if we re-executed within the
            // throttle window — the next submit (or pulse) will pick it up
            // after the window expires.
            if (reactivated && activeStreamCount > 0)
                return true;
            return isDirty() && activeStreamCount > 0 && !isWithinExecuteThrottle();
        }

        /**
         * True when the SQL was re-executed less than {@link #MIN_REFRESH_INTERVAL_MS}
         * ago. The cached `lastQueryResult` is treated as authoritative during
         * that window — submits still mark the query dirty but the actual
         * re-execution is deferred until the window expires.
         */
        boolean isWithinExecuteThrottle() {
            return lastQueryExecutionTime > 0
                && (now() - lastQueryExecutionTime) < MIN_REFRESH_INTERVAL_MS;
        }

        public boolean isDirty() {
            return dirtyTime() >= 0;
        }

        public long dirtyTime() {
            return lastPossibleChangeTime - lastQueryExecutionTime;
        }

        public void addStreamInfo(StreamInfo streamInfo) {
            synchronized (this) {
                streamInfos.add(streamInfo);
                if (streamInfo.isActive()) {
                    updateActiveStreamCount(streamInfo, true);
                    markAsReactivated();
                }
            }
        }

        public void removeStreamInfo(StreamInfo streamInfo) {
            synchronized (this) {
                if (streamInfos.remove(streamInfo)) {
                    if (streamInfo.isActive())
                        updateActiveStreamCount(streamInfo, false);
                }
            }
        }

        public int getActiveNewStreamCount() {
            return activeNewStreamCount;
        }

    /*
        void markBlankStreamsAsFilled() {
            synchronized (this) {
                // Moving the blank streams into the filled ones
                filledStreamInfos.addAll(blankStreamInfos);
                blankStreamInfos.clear();
            }
        }
    */

        void updateActiveStreamCount(StreamInfo streamInfo, boolean increment) {
            int delta = increment ? 1 : -1;
            activeStreamCount += delta;
            if (streamInfo.lastQueryResult != lastQueryResult)
                activeNewStreamCount += delta;
            streamInfo.childrenStreamInfos.forEach(csi -> {
                if (csi.active)
                    csi.queryInfo.updateActiveStreamCount(csi, increment);
            });
        }

        public boolean hasNoMoreStreams() {
            return streamInfos.isEmpty();
        }

        public DataScope getQueryScope() {
            return queryArgument.getDataScope();
        }
    }

    /**
     * Build the wire-side QueryResult for a push: strip column metadata the client already has
     * cached, and ship entityMapping only on the first push of each stream (the mapping is
     * invariant for a fixed query, so re-sending it on every push wastes proportional bytes —
     * especially for queries with deep FK chains or aggregate aliases).
     * <p>
     * The first push always carries entityMapping regardless of the client's {@code sendMetadata}
     * flag — defensive against the "cold cache lying about being warm" race that previously
     * stranded streams in positional-column-name mode for their whole lifetime. Subsequent
     * pushes omit it because the client (per-statement {@code QueryMetadataCache}) has it.
     * <p>
     * The `entityMappingSent` flag is only flipped once we actually emit a non-null mapping —
     * otherwise an early push that happened to lack entityMapping (e.g. the ORM layer skipped
     * computing it for some pre-data initial result) would mark the stream "done" without the
     * client ever seeing the mapping, stranding all later pushes in positional-name decode.
     */
    private QueryResult trimForPush(StreamInfo streamInfo, QueryResult queryResult) {
        if (queryResult == null) return null;
        QueryArgument qa = streamInfo.queryInfo.getQueryArgument();
        boolean firstPush = !streamInfo.entityMappingSent;
        boolean clientHasColumnNamesCached = qa != null && !qa.isSendMetadata();
        Object entityMapping = queryResult.getEntityMapping();
        boolean hasEntityMapping = entityMapping != null;
        if (firstPush && !clientHasColumnNamesCached) {
            // Cold client subscribe — let the full result through unchanged.
            if (hasEntityMapping) streamInfo.entityMappingSent = true;
            return queryResult;
        }
        QueryResult stripped = new QueryResult(queryResult.getRowCount(), queryResult.getColumnCount(), queryResult.getValues(), null);
        stripped.setVersionNumber(queryResult.getVersionNumber());
        stripped.setCallSeq(queryResult.getCallSeq());
        if (firstPush && hasEntityMapping) {
            stripped.setEntityMapping(entityMapping);
            streamInfo.entityMappingSent = true;
        }
        return stripped;
    }

    public final class StreamInfo {
        private final long creationTime = now();
        public Object queryStreamId;
        private StreamInfo parentStreamInfo;
        public final List<StreamInfo> childrenStreamInfos = new ArrayList<>();
        public final Object clientRunId;
        // Captured at subscription time for monitoring (may be null for anonymous clients; a user
        // logging in after opening the stream is not reflected — new streams will carry the userId).
        public final Object userId;
        // Subscriber origin for the /monitor BO/FO breakdown: TRUE=back-office, FALSE=front-office,
        // null=unknown. Captured on the subscribing client's thread (reliable for React clients).
        public final Boolean backoffice;
        public Boolean active;
        public Boolean close;
        public QueryInfo queryInfo;
        private QueryResult lastQueryResult;
        // Whether the client has already received entityMapping for this stream. Set to true on
        // the first push that ships metadata so subsequent pushes can omit it from the wire —
        // entityMapping doesn't change for a fixed query, so re-sending it on every push wastes
        // bytes proportional to query complexity (FK chains, alias names, etc.).
        private boolean entityMappingSent;

        public StreamInfo(QueryPushArgument arg) {
            queryStreamId = arg.getQueryStreamId();
            clientRunId = ThreadLocalStateHolder.getRunId();
            userId = ThreadLocalStateHolder.getUserId();
            backoffice = clientRunId == null ? null : ThreadLocalStateHolder.isBackoffice();
            Object parentQueryStreamId = arg.getParentQueryStreamId();
            parentStreamInfo = getStreamInfo(parentQueryStreamId);
            if (parentStreamInfo != null)
                parentStreamInfo.childrenStreamInfos.add(this);
            Console.log(">>> parentStreamInfoId = " + parentQueryStreamId + ", parentStreamInfo " + (parentStreamInfo == null ? "null" : "not null"));
            active = arg.getActive();
            close = arg.getClose();
            setStreamQueryArgument(this, arg.getQueryArgument());
        }

        public void setActive(boolean active) {
            if (this.active != active) {
                this.active = active;
                queryInfo.updateActiveStreamCount(this, active);
            }
        }

        public boolean isActive() {
            return active != null && active && (parentStreamInfo == null || parentStreamInfo.isActive());
        }

        public void markAsResend() {
            // Forgetting lastQueryResult will force to send the whole result on next push
            lastQueryResult = null;
        }
    }

    protected abstract StreamInfo getStreamInfo(Object queryStreamId);
}
