package dev.webfx.stack.db.querypush;

import dev.webfx.stack.db.query.QueryArgument;

import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public final class QueryPushArgument {

    private final Object queryStreamId;
    private final Object parentQueryStreamId;
    private final QueryArgument queryArgument;
    private final Object dataSourceId;
    private final Boolean active;
    private final Boolean resend;
    private final Boolean close;
    private final transient Consumer<QueryPushResult> queryPushResultConsumer;

    public QueryPushArgument(Object queryStreamId, Object parentQueryStreamId, QueryArgument queryArgument, Object dataSourceId, Boolean active, Boolean resend, Boolean close, Consumer<QueryPushResult> queryPushResultConsumer) {
        this.queryStreamId = queryStreamId;
        this.parentQueryStreamId = parentQueryStreamId;
        this.queryArgument = queryArgument;
        this.queryPushResultConsumer = queryPushResultConsumer;
        this.dataSourceId = dataSourceId != null || queryArgument == null ? dataSourceId : queryArgument.getDataSourceId();
        this.active = active;
        this.resend = resend;
        this.close = close;
    }

    public Object getQueryStreamId() {
        return queryStreamId;
    }

    public Object getParentQueryStreamId() {
        return parentQueryStreamId;
    }

    public QueryArgument getQueryArgument() {
        return queryArgument;
    }

    public Consumer<QueryPushResult> getQueryPushResultConsumer() {
        return queryPushResultConsumer;
    }

    public Object getDataSourceId() {
        return dataSourceId;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getResend() {
        return resend;
    }

    public Boolean getClose() {
        return close;
    }

    public boolean isOpenStreamArgument() {
        return queryStreamId == null;
    }

    public boolean isUpdateStreamArgument() {
        return queryStreamId != null && close == null;
    }

    public boolean isCloseStreamArgument() {
        return queryStreamId != null && close != null;
    }

    public static QueryPushArgumentBuilder builder() {
        return new QueryPushArgumentBuilder();
    }

    public static QueryPushArgument openStreamArgument(Object parentQueryStreamId, QueryArgument queryArgument, Consumer<QueryPushResult> queryResultConsumer) {
        return new QueryPushArgument(null, parentQueryStreamId, queryArgument, null, true, null, null, queryResultConsumer);
    }

    public static QueryPushArgument updateStreamArgument(Object queryStreamId, QueryArgument queryArgument) {
        return updateStreamArgument(queryStreamId, queryArgument, null);
    }

    public static QueryPushArgument updateStreamArgument(Object queryStreamId, QueryArgument queryArgument, Boolean active) {
        return updateStreamArgument(queryStreamId, queryArgument, queryArgument.getDataSourceId(), active);
    }

    public static QueryPushArgument updateStreamArgument(Object queryStreamId, Object dataSourceId, Boolean active) {
        return updateStreamArgument(queryStreamId, null, dataSourceId, active);
    }

    public static QueryPushArgument updateStreamArgument(Object queryStreamId, QueryArgument queryArgument, Object dataSourceId, Boolean active) {
        return new QueryPushArgument(queryStreamId, null, queryArgument, dataSourceId, active, null,null, null);
    }

    public static QueryPushArgument closeStreamArgument(Object queryStreamId, Object dataSourceId) {
        return new QueryPushArgument(queryStreamId, null, null, dataSourceId, null,null, true, null);
    }

}
