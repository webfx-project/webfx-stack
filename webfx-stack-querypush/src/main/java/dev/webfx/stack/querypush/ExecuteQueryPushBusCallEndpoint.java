package dev.webfx.stack.querypush;

import dev.webfx.stack.com.buscall.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class ExecuteQueryPushBusCallEndpoint extends AsyncFunctionBusCallEndpoint<QueryPushArgument, Object> {

    public ExecuteQueryPushBusCallEndpoint() {
        super(QueryPushService.QUERY_PUSH_SERVICE_ADDRESS, QueryPushService::executeQueryPush);
    }
}
