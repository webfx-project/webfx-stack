package dev.webfx.stack.db.querypush.buscall;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.querypush.QueryPushService;

/**
 * Bus endpoint resetting the SQL execution metrics (cumulative counters + per-statement rollup +
 * compression) so the /monitor page can measure a fresh window. Takes no argument; the reply is TRUE
 * on success. The provider returns null when not available — client-side providers, or a caller that
 * isn't a logged-in back-office user — in which case the call fails rather than replying.
 *
 * @author Bruno Salmon
 */
public final class ResetSqlMonitorMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, Boolean> {

    public ResetSqlMonitorMethodEndpoint() {
        super(QueryPushServiceBusAddress.RESET_SQL_MONITOR_METHOD_ADDRESS, ignoredArgument -> {
            Boolean result = QueryPushService.resetSqlMonitor();
            if (result == null)
                return Future.failedFuture(new IllegalStateException("Query push monitor reset is not available"));
            return Future.succeededFuture(result);
        });
    }
}
