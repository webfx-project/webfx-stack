package dev.webfx.stack.platform.shared.services.query.spi;

import dev.webfx.stack.platform.shared.services.query.QueryResult;
import dev.webfx.stack.platform.async.Batch;
import dev.webfx.stack.platform.async.Future;
import dev.webfx.stack.platform.shared.services.query.QueryArgument;

/**
 * @author Bruno Salmon
 */
public interface QueryServiceProvider {

    Future<QueryResult> executeQuery(QueryArgument argument);

    // Batch support

    default Future<Batch<QueryResult>> executeQueryBatch(Batch<QueryArgument> batch) {
        return batch.executeParallel(QueryResult[]::new, this::executeQuery);
    }

}
