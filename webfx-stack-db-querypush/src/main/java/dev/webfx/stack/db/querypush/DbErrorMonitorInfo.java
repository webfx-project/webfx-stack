package dev.webfx.stack.db.querypush;

/**
 * One recent failed SQL operation for the /monitor "Errors" drill-down — the wire form of
 * {@code SqlExecutionMonitor.ErrorSnapshot}. Newest first in the snapshot; the count on the Errors
 * card can exceed the number of rows kept (bounded ring buffer). Cleared by the same Clear action as
 * the execution counters.
 *
 * @author Bruno Salmon
 */
public final class DbErrorMonitorInfo {

    private final long epochMillis; // wall-clock time of the failure
    private final String kind;      // "read" (query) or "write" (submit)
    private final String statement; // the failing statement (a batch reports its first)
    private final String message;   // the cause message (already truncated server-side)
    private final String origin;    // "bo" / "fo" / null — the caller's app type

    public DbErrorMonitorInfo(long epochMillis, String kind, String statement, String message, String origin) {
        this.epochMillis = epochMillis;
        this.kind = kind;
        this.statement = statement;
        this.message = message;
        this.origin = origin;
    }

    public long getEpochMillis() { return epochMillis; }
    public String getKind() { return kind; }
    public String getStatement() { return statement; }
    public String getMessage() { return message; }
    public String getOrigin() { return origin; }
}
