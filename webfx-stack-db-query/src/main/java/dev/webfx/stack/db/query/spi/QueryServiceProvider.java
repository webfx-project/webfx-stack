package dev.webfx.stack.db.query.spi;

import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.stack.db.query.QueryArgument;

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
