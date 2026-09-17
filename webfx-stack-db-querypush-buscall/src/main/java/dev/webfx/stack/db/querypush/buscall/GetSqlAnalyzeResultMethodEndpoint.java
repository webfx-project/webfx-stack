package dev.webfx.stack.db.querypush.buscall;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.querypush.QueryPushService;
import dev.webfx.stack.db.querypush.SqlAnalyzeResultInfo;

/**
 * Bus endpoint returning the current analyze state for a statement (pending / ready-with-plan /
 * none) — polled by the /monitor page after arming. The string argument is the statement. The
 * provider returns null when analyze isn't available (client-side providers, or a caller that
 * isn't a logged-in back-office user), in which case the call fails rather than replying.
 *
 * @author Bruno Salmon
 */
public final class GetSqlAnalyzeResultMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, SqlAnalyzeResultInfo> {

    public GetSqlAnalyzeResultMethodEndpoint() {
        super(QueryPushServiceBusAddress.GET_SQL_ANALYZE_RESULT_METHOD_ADDRESS, argument -> {
            if (!(argument instanceof String))
                return Future.failedFuture(new IllegalArgumentException("getSqlAnalyzeResult expects a statement string"));
            SqlAnalyzeResultInfo result = QueryPushService.getSqlAnalyzeResult((String) argument);
            if (result == null)
                return Future.failedFuture(new IllegalStateException("Query analyze is not available"));
            return Future.succeededFuture(result);
        });
    }
}
