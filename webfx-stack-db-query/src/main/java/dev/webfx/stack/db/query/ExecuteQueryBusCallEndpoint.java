package dev.webfx.stack.db.query;

import dev.webfx.stack.com.buscall.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class ExecuteQueryBusCallEndpoint extends AsyncFunctionBusCallEndpoint<QueryArgument, QueryResult> {

    public ExecuteQueryBusCallEndpoint() {
        super(QueryService.QUERY_SERVICE_ADDRESS, QueryService::executeQuery);
    }
}
