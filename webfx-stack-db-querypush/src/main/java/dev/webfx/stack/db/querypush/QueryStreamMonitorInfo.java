package dev.webfx.stack.db.querypush;

/**
 * Read-only monitoring snapshot of one push query registered on the server (one distinct
 * {@link dev.webfx.stack.db.query.QueryArgument}, possibly subscribed by several client streams).
 * Instances are produced by {@link QueryPushService#getMonitorInfo()} and are meant for display
 * in an administration console — they carry no live references to the server internals.
 *
 * @author Bruno Salmon
 */
public final class QueryStreamMonitorInfo {

    private final Object[] queryStreamIds;
    private final String statement;
    private final String parameters; // compact display string (e.g. "[1857, 42]"), null when the query has no parameters
    private final int rowCount; // -1 when the query hasn't been executed yet (no result pushed so far)
    private final int streamCount;
    private final int activeStreamCount;
    private final int clientsCount; // distinct client runIds (~ browser tabs) subscribed to this query
    private final int usersCount; // distinct logged-in users subscribed to this query
    private final long lastExecutionAgeMillis; // -1 when the query hasn't been executed yet
    private final String origin; // "bo" / "fo" / "both" / null — client type(s) of the subscribers

    public QueryStreamMonitorInfo(Object[] queryStreamIds, String statement, String parameters, int rowCount, int streamCount, int activeStreamCount, int clientsCount, int usersCount, long lastExecutionAgeMillis, String origin) {
        this.queryStreamIds = queryStreamIds;
        this.statement = statement;
        this.parameters = parameters;
        this.rowCount = rowCount;
        this.streamCount = streamCount;
        this.activeStreamCount = activeStreamCount;
        this.clientsCount = clientsCount;
        this.usersCount = usersCount;
        this.lastExecutionAgeMillis = lastExecutionAgeMillis;
        this.origin = origin;
    }

    public Object[] getQueryStreamIds() {
        return queryStreamIds;
    }

    public String getStatement() {
        return statement;
    }

    public String getParameters() {
        return parameters;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getStreamCount() {
        return streamCount;
    }

    public int getActiveStreamCount() {
        return activeStreamCount;
    }

    public int getClientsCount() {
        return clientsCount;
    }

    public int getUsersCount() {
        return usersCount;
    }

    public long getLastExecutionAgeMillis() {
        return lastExecutionAgeMillis;
    }

    public String getOrigin() {
        return origin;
    }
}
