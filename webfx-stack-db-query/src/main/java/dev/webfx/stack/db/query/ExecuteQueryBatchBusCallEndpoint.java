package dev.webfx.stack.db.query;

import dev.webfx.stack.com.buscall.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.async.Batch;

/**
 * @author Bruno Salmon
 */
public final class ExecuteQueryBatchBusCallEndpoint extends AsyncFunctionBusCallEndpoint<Batch<QueryArgument>, Batch<QueryResult>> {

    public ExecuteQueryBatchBusCallEndpoint() {
        super(QueryService.QUERY_BATCH_SERVICE_ADDRESS, QueryService::executeQueryBatch);
    }
}
