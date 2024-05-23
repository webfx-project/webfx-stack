package dev.webfx.stack.db.submit.spi.impl.vertx;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;

import java.net.SocketException;

/**
 *  * This class is designed to fix a connection issue we are experiencing with a remote Postgres database, where
 *  * some connections in the pool may be broken (using them raises a SocketException, usually with message
 *  * "Connection reset"). This class catches that exception, and retry the same operation again until eventually
 *  * it succeeds with a good connection.
 *
 * @author Bruno Salmon
 */
final class RetryOnSocketExceptionSubmitServiceProvider implements SubmitServiceProvider {

    private final int MAX_RETRY_COUNT = 40;

    private final SubmitServiceProvider submitServiceProvider;

    public RetryOnSocketExceptionSubmitServiceProvider(SubmitServiceProvider submitServiceProvider) {
        this.submitServiceProvider = submitServiceProvider;
    }

    @Override
    public Future<SubmitResult> executeSubmit(SubmitArgument argument) {
        return executeSubmit(argument, 0);
    }

    public Future<SubmitResult> executeSubmit(SubmitArgument argument, int retryCount) {
        return submitServiceProvider.executeSubmit(argument)
                .recover(cause -> {
                    if (!(cause instanceof SocketException) || retryCount >= MAX_RETRY_COUNT)
                        return Future.failedFuture(cause);
                    Console.log("Retrying executeSubmit() after SocketException (retryCount = " + (retryCount + 1) + ")");
                    return executeSubmit(argument, retryCount + 1);
                });
    }

    @Override
    public Future<Batch<SubmitResult>> executeSubmitBatch(Batch<SubmitArgument> batch) {
        return executeSubmitBatch(batch, 0);
    }

    public Future<Batch<SubmitResult>> executeSubmitBatch(Batch<SubmitArgument> batch, int retryCount) {
        return submitServiceProvider.executeSubmitBatch(batch)
                .recover(cause -> {
                    if (!(cause instanceof SocketException) || retryCount >= MAX_RETRY_COUNT)
                        return Future.failedFuture(cause);
                    Console.log("Retrying executeSubmitBatch() after SocketException (retryCount = " + (retryCount + 1) + ")");
                    return executeSubmitBatch(batch, retryCount + 1);
                });
    }
}
