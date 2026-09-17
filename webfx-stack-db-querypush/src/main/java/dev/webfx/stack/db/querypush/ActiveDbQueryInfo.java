package dev.webfx.stack.db.querypush;

/**
 * One currently-active (non-idle) backend on the database, for the /monitor "Database" drill-down —
 * read from {@code pg_stat_activity}. Surfaces long-running and blocking queries across ALL clients
 * (this server, KBS2, admin tools, autovacuum), which the server-only in-flight list can't see.
 *
 * @author Bruno Salmon
 */
public final class ActiveDbQueryInfo {

    private final int pid;                 // backend process id
    private final long durationMillis;     // how long the current query has been running (now - query_start)
    private final String state;            // pg_stat_activity.state (e.g. "active", "idle in transaction")
    private final String waitEventType;    // wait_event_type when waiting (e.g. "Lock", "IO"), or null
    private final int blockedByPid;        // pid blocking this one (from pg_blocking_pids), 0 = not blocked
    private final String query;            // the SQL text (truncated server-side)

    public ActiveDbQueryInfo(int pid, long durationMillis, String state, String waitEventType, int blockedByPid, String query) {
        this.pid = pid;
        this.durationMillis = durationMillis;
        this.state = state;
        this.waitEventType = waitEventType;
        this.blockedByPid = blockedByPid;
        this.query = query;
    }

    public int getPid() { return pid; }
    public long getDurationMillis() { return durationMillis; }
    public String getState() { return state; }
    public String getWaitEventType() { return waitEventType; }
    public int getBlockedByPid() { return blockedByPid; }
    public String getQuery() { return query; }
}
