package dev.webfx.stack.db.query.buscall;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryService;

/**
 * @author Bruno Salmon
 */
public final class ExecuteQueryMethodEndpoint extends AsyncFunctionBusCallEndpoint<QueryArgument, QueryResult> {

    public ExecuteQueryMethodEndpoint() {
        super(QueryServiceBusAddress.EXECUTE_QUERY_METHOD_ADDRESS, QueryService::executeQuery);
    }
}
