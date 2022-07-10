package dev.webfx.stack.framework.shared.services.querypush;

import dev.webfx.stack.platform.shared.services.buscall.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class ExecuteQueryPushBusCallEndpoint extends AsyncFunctionBusCallEndpoint<QueryPushArgument, Object> {

    public ExecuteQueryPushBusCallEndpoint() {
        super(QueryPushService.QUERY_PUSH_SERVICE_ADDRESS, QueryPushService::executeQueryPush);
    }
}
