package dev.webfx.stack.db.query.spi.impl.vertx;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;

import java.net.SocketException;

/**
 * This class is designed to fix a connection issue we are experiencing with a remote Postgres database, where
 * some connections in the pool may be broken (using them raises a SocketException, usually with message
 * "Connection reset"). This class catches that exception, and retry the same operation again until eventually
 * it succeeds with a good connection.
 *
 * @author Bruno Salmon
 */
final class RetryOnSocketExceptionQueryServiceProvider implements QueryServiceProvider {

    private final int MAX_RETRY_COUNT = 40;

    private final QueryServiceProvider queryServiceProvider;

    public RetryOnSocketExceptionQueryServiceProvider(QueryServiceProvider queryServiceProvider) {
        this.queryServiceProvider = queryServiceProvider;
    }

    @Override
    public Future<QueryResult> executeQuery(QueryArgument argument) {
        return executeQuery(argument, 0);
    }

    private Future<QueryResult> executeQuery(QueryArgument argument, int retryCount) {
        return queryServiceProvider.executeQuery(argument)
                .recover(cause -> {
                    if (!(cause instanceof SocketException) || retryCount >= MAX_RETRY_COUNT)
                        return Future.failedFuture(cause);
                    Console.log("Retrying executeQuery() after SocketException (retryCount = " + (retryCount + 1) + ")");
                    return executeQuery(argument, retryCount + 1);
                });
    }

        @Override
    public Future<Batch<QueryResult>> executeQueryBatch(Batch<QueryArgument> batch) {
        return executeQueryBatch(batch, 0);
    }

    private Future<Batch<QueryResult>> executeQueryBatch(Batch<QueryArgument> batch, int retryCount) {
        return queryServiceProvider.executeQueryBatch(batch)
                .recover(cause -> {
                    if (!(cause instanceof SocketException) || retryCount >= MAX_RETRY_COUNT)
                        return Future.failedFuture(cause);
                    Console.log("Retrying executeQueryBatch() after SocketException (retryCount = " + (retryCount + 1) + ")");
                    return executeQueryBatch(batch, retryCount + 1);
                });
    }
}
