package dev.webfx.stack.db.query.buscall;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.platform.async.Batch;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryService;

/**
 * @author Bruno Salmon
 */
public final class ExecuteQueryBatchMethodEndpoint extends AsyncFunctionBusCallEndpoint<Batch<QueryArgument>, Batch<QueryResult>> {

    public ExecuteQueryBatchMethodEndpoint() {
        super(QueryServiceBusAddress.EXECUTE_QUERY_BATCH_METHOD_ADDRESS, QueryService::executeQueryBatch);
    }
}
