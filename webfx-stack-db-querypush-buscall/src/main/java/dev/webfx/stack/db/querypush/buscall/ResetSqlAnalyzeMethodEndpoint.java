package dev.webfx.stack.db.querypush.buscall;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.querypush.QueryPushService;

/**
 * Bus endpoint dropping one statement's analyze arm + captured plan (the /monitor per-row "Reset"),
 * so its row returns to "Analyze". The string argument is the statement; the reply is TRUE. The
 * provider returns null when analyze isn't available — client-side providers, or a caller that isn't
 * a logged-in back-office user — in which case the call fails rather than replying.
 *
 * @author Bruno Salmon
 */
public final class ResetSqlAnalyzeMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, Boolean> {

    public ResetSqlAnalyzeMethodEndpoint() {
        super(QueryPushServiceBusAddress.RESET_SQL_ANALYZE_METHOD_ADDRESS, argument -> {
            if (!(argument instanceof String))
                return Future.failedFuture(new IllegalArgumentException("resetSqlAnalyze expects a statement string"));
            Boolean result = QueryPushService.resetSqlAnalyze((String) argument);
            if (result == null)
                return Future.failedFuture(new IllegalStateException("Query analyze is not available"));
            return Future.succeededFuture(result);
        });
    }
}
