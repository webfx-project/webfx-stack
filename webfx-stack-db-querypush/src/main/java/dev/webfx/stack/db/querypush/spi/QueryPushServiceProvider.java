package dev.webfx.stack.db.querypush.spi;

import dev.webfx.stack.db.querypush.DatabaseHealthMonitorInfo;
import dev.webfx.stack.db.querypush.PulseArgument;
import dev.webfx.stack.db.querypush.QueryPushArgument;
import dev.webfx.stack.db.querypush.QueryPushMonitorInfo;
import dev.webfx.stack.db.querypush.SqlAnalyzeResultInfo;
import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface QueryPushServiceProvider {

    Future<Object> executeQueryPush(QueryPushArgument argument);

    void executePulse(PulseArgument argument);

    /**
     * Returns a monitoring snapshot of the query-push state (registered push clients + active
     * push queries with their subscription details), or null when not available — which is the
     * case on client-side providers, and on the server when the caller isn't a logged-in user.
     * <p>
     * Deliberately NOT a defaulted method: it must be implemented explicitly by every provider,
     * <em>including decorators</em> (e.g. the DQL scope interceptor wrapper), so a decorator that
     * forgets to delegate is a compile error rather than a silent null at runtime.
     */
    QueryPushMonitorInfo getMonitorInfo();

    /**
     * Read-only database health snapshot (connections + non-idle/long-running/blocking queries) for
     * the /monitor "Database" drill-down, fetched on demand (queries {@code pg_stat_activity}). Fails
     * the future when not available (client-side providers, or a caller that isn't a logged-in user).
     * <p>
     * Deliberately NOT defaulted (like {@link #getMonitorInfo()}) so decorators must delegate.
     */
    Future<DatabaseHealthMonitorInfo> getDatabaseHealthInfo();

    /**
     * Requests best-effort cancellation of an in-flight SQL query by its monitor id (as reported
     * in {@code getMonitorInfo().sqlExecution.inFlight}). Returns TRUE when a cancel action was
     * dispatched, FALSE when the id is unknown / already finished / not cancellable, and null when
     * not available (client-side providers, or a server caller that isn't a logged-in user).
     * <p>
     * Like {@link #getMonitorInfo()}, deliberately NOT a defaulted method so decorators must
     * delegate explicitly. Cancellation is out-of-band and advisory: the query may finish first,
     * and a cancelled query fails (SQLSTATE 57014) with its transaction rolled back.
     */
    Boolean cancelSqlQuery(long monitorId);

    /**
     * Arms a tracked read statement for "analyze on next occurrence": the next time the server runs
     * that exact statement it captures the real parameters and EXPLAINs it (see
     * {@link #getSqlAnalyzeResult(String)}). Returns TRUE when armed, FALSE when the statement isn't
     * a known read statement (so arbitrary SQL can't be armed), and null when not available
     * (client-side providers, or a caller that isn't a logged-in user).
     * <p>
     * Deliberately NOT defaulted (like {@link #getMonitorInfo()}) so decorators must delegate.
     */
    Boolean armSqlAnalyze(String statement);

    /**
     * Returns the current analyze state for a statement (pending / ready-with-plan / none), or null
     * when not available (client-side providers, or a caller that isn't a logged-in user).
     * <p>
     * Deliberately NOT defaulted (like {@link #getMonitorInfo()}) so decorators must delegate.
     */
    SqlAnalyzeResultInfo getSqlAnalyzeResult(String statement);

    /**
     * Drops one statement's analyze arm and captured plan (the /monitor per-row "Reset"), so its row
     * returns to "Analyze". Returns TRUE when handled, and null when not available (client-side
     * providers, or a caller that isn't a logged-in user).
     * <p>
     * Deliberately NOT defaulted (like {@link #getMonitorInfo()}) so decorators must delegate.
     */
    Boolean resetSqlAnalyze(String statement);

    /**
     * Resets the SQL execution metrics (cumulative counters + per-statement rollup + compression)
     * so the /monitor page can measure a fresh window. Returns TRUE when reset, and null when not
     * available (client-side providers, or a caller that isn't a logged-in user). Live state (queues,
     * in-flight queries) is kept.
     * <p>
     * Deliberately NOT defaulted (like {@link #getMonitorInfo()}) so decorators must delegate.
     */
    Boolean resetSqlMonitor();

}
