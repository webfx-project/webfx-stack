package dev.webfx.stack.db.querypush.buscall;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.querypush.QueryPushMonitorInfo;
import dev.webfx.stack.db.querypush.QueryPushService;

/**
 * Bus endpoint exposing the query-push monitoring snapshot (connected push clients + active
 * push queries) to administration consoles. The provider returns null when the snapshot isn't
 * available — client-side providers, or a caller that isn't a logged-in back-office user — in
 * which case the call fails rather than replying with an empty payload.
 *
 * @author Bruno Salmon
 */
public final class GetQueryPushMonitorInfoMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, QueryPushMonitorInfo> {

    public GetQueryPushMonitorInfoMethodEndpoint() {
        super(QueryPushServiceBusAddress.GET_MONITOR_INFO_METHOD_ADDRESS, ignoredArgument -> {
            QueryPushMonitorInfo monitorInfo = QueryPushService.getMonitorInfo();
            if (monitorInfo == null)
                return Future.failedFuture(new IllegalStateException("Query push monitor info is not available"));
            return Future.succeededFuture(monitorInfo);
        });
    }
}
