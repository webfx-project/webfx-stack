package dev.webfx.stack.db.querypush.buscall;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.querypush.QueryPushService;

/**
 * Bus endpoint requesting best-effort cancellation of an in-flight SQL query by its monitor id
 * (as reported in the /monitor in-flight list). The numeric argument is the monitor id; the reply
 * is TRUE when a cancel was dispatched and FALSE when the id is unknown / already finished. The
 * provider returns null when cancellation isn't available — client-side providers, or a caller
 * that isn't a logged-in back-office user — in which case the call fails rather than replying.
 *
 * @author Bruno Salmon
 */
public final class CancelSqlQueryMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, Boolean> {

    public CancelSqlQueryMethodEndpoint() {
        super(QueryPushServiceBusAddress.CANCEL_SQL_QUERY_METHOD_ADDRESS, argument -> {
            if (!(argument instanceof Number))
                return Future.failedFuture(new IllegalArgumentException("cancelSqlQuery expects a numeric monitor id"));
            Boolean result = QueryPushService.cancelSqlQuery(((Number) argument).longValue());
            if (result == null)
                return Future.failedFuture(new IllegalStateException("Query cancellation is not available"));
            return Future.succeededFuture(result);
        });
    }
}
