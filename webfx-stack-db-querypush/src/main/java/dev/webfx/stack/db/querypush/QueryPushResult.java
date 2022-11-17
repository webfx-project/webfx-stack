package dev.webfx.stack.db.querypush;

import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.querypush.diff.QueryResultDiff;

/**
 * @author Bruno Salmon
 */
public final class QueryPushResult {

    private final Object queryStreamId;
    private final QueryResult queryResult;
    private final QueryResultDiff queryResultDiff;

    public QueryPushResult(Object queryStreamId, QueryResult queryResult) {
        this(queryStreamId, queryResult, null);
    }

    public QueryPushResult(Object queryStreamId, QueryResultDiff queryResultDiff) {
        this(queryStreamId, null, queryResultDiff);
    }

    public QueryPushResult(Object queryStreamId, QueryResult queryResult, QueryResultDiff queryResultDiff) {
        this.queryStreamId = queryStreamId;
        this.queryResult = queryResultDiff != null ? null : queryResult;
        this.queryResultDiff = queryResultDiff;
    }

    public Object getQueryStreamId() {
        return queryStreamId;
    }

    public QueryResult getQueryResult() {
        return queryResult;
    }

    public QueryResultDiff getQueryResultDiff() {
        return queryResultDiff;
    }

}
