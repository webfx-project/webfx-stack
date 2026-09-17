package dev.webfx.stack.db.querypush;

/**
 * Per-statement SQL execution rollup for the /monitor "slow queries" drill-down — the wire form
 * of {@code SqlExecutionMonitor.StatementSnapshot}. Statements are already parameter-free
 * ($1, $2 …), so each row aggregates all executions of the same statement. Ranked by total time.
 *
 * @author Bruno Salmon
 */
public final class StatementMonitorInfo {

    private final String statement;
    private final String kind;      // "read" (query) or "write" (submit)
    private final long count;       // executions
    private final long totalNanos;  // total execution wall time
    private final long maxNanos;    // slowest single execution
    private final String origin;    // "bo" / "fo" / "both" / null — client type(s) that ran it (accumulated)

    public StatementMonitorInfo(String statement, String kind, long count, long totalNanos, long maxNanos, String origin) {
        this.statement = statement;
        this.kind = kind;
        this.count = count;
        this.totalNanos = totalNanos;
        this.maxNanos = maxNanos;
        this.origin = origin;
    }

    public String getStatement() { return statement; }
    public String getKind() { return kind; }
    public long getCount() { return count; }
    public long getTotalNanos() { return totalNanos; }
    public long getMaxNanos() { return maxNanos; }
    public String getOrigin() { return origin; }
}
