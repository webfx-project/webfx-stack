package dev.webfx.framework.shared.services.querypush;

import dev.webfx.platform.shared.services.buscall.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class ExecuteQueryPushBusCallEndpoint extends AsyncFunctionBusCallEndpoint<QueryPushArgument, Object> {

    public ExecuteQueryPushBusCallEndpoint() {
        super(QueryPushService.QUERY_PUSH_SERVICE_ADDRESS, QueryPushService::executeQueryPush);
    }
}
