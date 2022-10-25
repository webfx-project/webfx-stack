package dev.webfx.stack.querypush.buscall;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.querypush.QueryPushArgument;
import dev.webfx.stack.querypush.QueryPushService;

/**
 * @author Bruno Salmon
 */
public final class ExecuteQueryPushMethodEndpoint extends AsyncFunctionBusCallEndpoint<QueryPushArgument, Object> {

    public ExecuteQueryPushMethodEndpoint() {
        super(QueryPushServiceBusAddress.EXECUTE_QUERY_PUSH_METHOD_ADDRESS, QueryPushService::executeQueryPush);
    }
}
