package dev.webfx.stack.platform.shared.services.query;

import dev.webfx.stack.platform.shared.services.buscall.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.platform.async.Batch;

/**
 * @author Bruno Salmon
 */
public final class ExecuteQueryBatchBusCallEndpoint extends AsyncFunctionBusCallEndpoint<Batch<QueryArgument>, Batch<QueryResult>> {

    public ExecuteQueryBatchBusCallEndpoint() {
        super(QueryService.QUERY_BATCH_SERVICE_ADDRESS, QueryService::executeQueryBatch);
    }
}
