package dev.webfx.stack.db.query;

import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.service.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class QueryService {

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
