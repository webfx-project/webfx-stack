package dev.webfx.stack.db.query;

import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.async.Batch;
import dev.webfx.stack.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class QueryService {

    public static final String QUERY_SERVICE_ADDRESS = "service/query";
    public static final String QUERY_BATCH_SERVICE_ADDRESS = "service/query/batch";

    public static QueryServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(QueryServiceProvider.class, () -> ServiceLoader.load(QueryServiceProvider.class));
    }

    public static Future<QueryResult> executeQuery(QueryArgument argument) {
        return getProvider().executeQuery(argument);
    }

    // Batch support

    public static Future<Batch<QueryResult>> executeQueryBatch(Batch<QueryArgument> batch) {
        return getProvider().executeQueryBatch(batch);
    }

}
