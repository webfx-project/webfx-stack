package dev.webfx.stack.db.querypush;

/**
 * Read-only database health snapshot for the /monitor "Database" drill-down, built from a single
 * {@code pg_stat_activity} query: the connection count (active / idle / total vs {@code
 * max_connections}) and the list of non-idle / long-running / blocking backends, read from one
 * sample so the two can never disagree. Fetched on demand when the drill-down opens (NOT on the
 * regular monitor poll), since it queries the database.
 *
 * @author Bruno Salmon
 */
public final class DatabaseHealthMonitorInfo {

    private final int maxConnections;      // server's max_connections setting (-1 if unknown)
    private final int totalConnections;    // total backends currently connected (all clients)
    private final int activeConnections;   // backends with state = 'active'
    private final int idleConnections;     // backends with state = 'idle'
    // Client backends whose state reads NULL, i.e. sessions this database role is not allowed to
    // inspect (Postgres blanks state/query/query_start unless you are a superuser or a member of
    // pg_read_all_stats / pg_monitor). Their queries CANNOT appear in activeQueries, so a non-zero
    // value here is the honest caveat on that list: "N backends exist that I cannot show you".
    private final int notInspectableConnections;
    // The connection this snapshot is itself using — one backend, since the snapshot is a single
    // statement, and 'active' only because we asked. Counted here and excluded from
    // activeConnections and from activeQueries, so both report real CLIENT work rather than the
    // monitor watching itself. Included in totalConnections: it holds a real connection slot.
    // Another server task's or another admin's health query is not this — it counts as client work.
    private final int selfConnections;
    private final ActiveDbQueryInfo[] activeQueries; // non-idle backends, worst-first

    public DatabaseHealthMonitorInfo(int maxConnections, int totalConnections, int activeConnections,
                                     int idleConnections, int notInspectableConnections,
                                     int selfConnections, ActiveDbQueryInfo[] activeQueries) {
        this.maxConnections = maxConnections;
        this.totalConnections = totalConnections;
        this.activeConnections = activeConnections;
        this.idleConnections = idleConnections;
        this.notInspectableConnections = notInspectableConnections;
        this.selfConnections = selfConnections;
        this.activeQueries = activeQueries;
    }

    public int getMaxConnections() { return maxConnections; }
    public int getTotalConnections() { return totalConnections; }
    public int getActiveConnections() { return activeConnections; }
    public int getIdleConnections() { return idleConnections; }
    public int getNotInspectableConnections() { return notInspectableConnections; }
    public int getSelfConnections() { return selfConnections; }
    public ActiveDbQueryInfo[] getActiveQueries() { return activeQueries; }
}
