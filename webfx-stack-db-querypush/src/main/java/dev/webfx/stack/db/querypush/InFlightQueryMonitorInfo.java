package dev.webfx.stack.db.querypush;

/**
 * A currently-executing SQL statement for the /monitor "running queries" drill-down — the wire
 * form of {@code SqlExecutionMonitor.InFlightSnapshot}. The {@code id} identifies the execution
 * for a future cancellation feature.
 *
 * @author Bruno Salmon
 */
public final class InFlightQueryMonitorInfo {

    private final long id;
    private final String kind;      // "read" (query) or "write" (submit)
    private final String statement;
    private final long ageMillis;   // how long it has been running
    private final String origin;    // "bo" / "fo" / null — the executing client's type

    public InFlightQueryMonitorInfo(long id, String kind, String statement, long ageMillis, String origin) {
        this.id = id;
        this.kind = kind;
        this.statement = statement;
        this.ageMillis = ageMillis;
        this.origin = origin;
    }

    public long getId() { return id; }
    public String getKind() { return kind; }
    public String getStatement() { return statement; }
    public long getAgeMillis() { return ageMillis; }
    public String getOrigin() { return origin; }
}
