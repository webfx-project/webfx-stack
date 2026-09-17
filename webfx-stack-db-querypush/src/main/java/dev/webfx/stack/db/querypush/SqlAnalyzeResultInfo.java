package dev.webfx.stack.db.querypush;

/**
 * State of an "analyze this statement on its next occurrence" request for the /monitor slow-query
 * drill-down — the wire form of {@code SqlAnalyzeRegistry.Result}.
 * <p>
 * {@code status} is {@code "pending"} while waiting for the next occurrence (or capturing the plan),
 * {@code "ready"} once the plan is captured, and {@code "none"} when nothing is armed/captured for
 * the statement (or the arm expired). When ready, {@code plan} holds the {@code EXPLAIN (ANALYZE,
 * BUFFERS)} text, {@code dql} the original DQL the SQL was compiled from (null when not DQL-derived),
 * {@code parameters} the real parameters it used, and {@code capturedAgeMillis} the time since
 * capture; otherwise those are null / -1.
 *
 * @author Bruno Salmon
 */
public final class SqlAnalyzeResultInfo {

    private final String statement;         // the analyzed statement — null for a single-statement poll, set in the snapshot list
    private final String status;            // "pending" | "ready" | "none"
    private final String plan;              // EXPLAIN output, non-null only when ready
    private final String dql;               // original DQL, null unless ready and DQL-derived
    private final String parameters;        // display of the captured parameters, may be null
    private final long capturedAgeMillis;   // ms since capture when ready, else -1
    private final String clientVersion;     // client build version that ran the captured occurrence, null when unknown

    public SqlAnalyzeResultInfo(String statement, String status, String plan, String dql, String parameters, long capturedAgeMillis, String clientVersion) {
        this.statement = statement;
        this.status = status;
        this.plan = plan;
        this.dql = dql;
        this.parameters = parameters;
        this.capturedAgeMillis = capturedAgeMillis;
        this.clientVersion = clientVersion;
    }

    public String getStatement() { return statement; }
    public String getStatus() { return status; }
    public String getPlan() { return plan; }
    public String getDql() { return dql; }
    public String getParameters() { return parameters; }
    public long getCapturedAgeMillis() { return capturedAgeMillis; }
    /** Client build version that ran the captured occurrence (READY only), or null when unknown. */
    public String getClientVersion() { return clientVersion; }
}
