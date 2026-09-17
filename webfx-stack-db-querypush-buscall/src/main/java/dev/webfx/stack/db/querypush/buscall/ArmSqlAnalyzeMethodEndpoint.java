package dev.webfx.stack.db.querypush.buscall;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.querypush.QueryPushService;

/**
 * Bus endpoint arming a read statement for "analyze on next occurrence" (see the /monitor
 * slow-query drill-down). The string argument is the statement to arm; the reply is TRUE when
 * armed and FALSE when it isn't a known read statement (so arbitrary SQL can't be armed). The
 * provider returns null when analyze isn't available — client-side providers, or a caller that
 * isn't a logged-in back-office user — in which case the call fails rather than replying.
 *
 * @author Bruno Salmon
 */
public final class ArmSqlAnalyzeMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, Boolean> {

    public ArmSqlAnalyzeMethodEndpoint() {
        super(QueryPushServiceBusAddress.ARM_SQL_ANALYZE_METHOD_ADDRESS, argument -> {
            if (!(argument instanceof String))
                return Future.failedFuture(new IllegalArgumentException("armSqlAnalyze expects a statement string"));
            Boolean result = QueryPushService.armSqlAnalyze((String) argument);
            if (result == null)
                return Future.failedFuture(new IllegalStateException("Query analyze is not available"));
            return Future.succeededFuture(result);
        });
    }
}
