package dev.webfx.stack.db.querypush;

import dev.webfx.stack.db.querypush.spi.QueryPushServiceProvider;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.service.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class QueryPushService {

    public static QueryPushServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(QueryPushServiceProvider.class, () -> ServiceLoader.load(QueryPushServiceProvider.class));
    }

    public static Future<Object> executeQueryPush(QueryPushArgument argument) {
        return getProvider().executeQueryPush(argument);
    }

    public static void executePulse(PulseArgument argument) {
        getProvider().executePulse(argument);
    }

    /**
     * Returns a monitoring snapshot of the query-push state (see
     * {@link QueryPushServiceProvider#getMonitorInfo()}), or null when not available.
     */
    public static QueryPushMonitorInfo getMonitorInfo() {
        return getProvider().getMonitorInfo();
    }

    /**
     * Returns the read-only database health snapshot (see
     * {@link QueryPushServiceProvider#getDatabaseHealthInfo()}); the future fails when not available.
     */
    public static Future<DatabaseHealthMonitorInfo> getDatabaseHealthInfo() {
        return getProvider().getDatabaseHealthInfo();
    }

    /**
     * Requests best-effort cancellation of an in-flight SQL query by its monitor id (see
     * {@link QueryPushServiceProvider#cancelSqlQuery(long)}). Returns TRUE if dispatched, FALSE if
     * unknown / already finished, or null when not available.
     */
    public static Boolean cancelSqlQuery(long monitorId) {
        return getProvider().cancelSqlQuery(monitorId);
    }

    /**
     * Arms a read statement for analyze-on-next-occurrence (see
     * {@link QueryPushServiceProvider#armSqlAnalyze(String)}).
     */
    public static Boolean armSqlAnalyze(String statement) {
        return getProvider().armSqlAnalyze(statement);
    }

    /**
     * Returns the current analyze state for a statement (see
     * {@link QueryPushServiceProvider#getSqlAnalyzeResult(String)}).
     */
    public static SqlAnalyzeResultInfo getSqlAnalyzeResult(String statement) {
        return getProvider().getSqlAnalyzeResult(statement);
    }

    /**
     * Drops one statement's analyze arm + captured plan (see
     * {@link QueryPushServiceProvider#resetSqlAnalyze(String)}).
     */
    public static Boolean resetSqlAnalyze(String statement) {
        return getProvider().resetSqlAnalyze(statement);
    }

    /**
     * Resets the SQL execution metrics for a fresh /monitor window (see
     * {@link QueryPushServiceProvider#resetSqlMonitor()}).
     */
    public static Boolean resetSqlMonitor() {
        return getProvider().resetSqlMonitor();
    }

}
