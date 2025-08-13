package dev.webfx.stack.db.querysubmit;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.util.AsyncQueue;
import dev.webfx.platform.shutdown.Shutdown;
import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.vertx.common.VertxInstance;
import dev.webfx.stack.db.datasource.ConnectionDetails;
import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.db.submit.GeneratedKeyReference;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.listener.SubmitListenerService;
import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static dev.webfx.platform.vertx.common.VertxFuture.toVertxFuture;
import static dev.webfx.platform.vertx.common.VertxFuture.toWebfxFuture;
import static dev.webfx.stack.db.querysubmit.VertxSqlUtil.*;

/**
 * Note: the same class is used for both the QueryService and the SubmitService, so there will be 2 different instances,
 * one for each service.
 *
 * @author Bruno Salmon
 */
public class VertxLocalPostgresQuerySubmitServiceProvider implements QueryServiceProvider, SubmitServiceProvider {

    private static final boolean LOG_TIMINGS = true;
    private static final long WARNING_MILLIS = 5000;
    private static final int POOL_SIZE = 10; // Note: it's for each instance (1 pool for queries = reading, another one for submissions = writing)

    private final Pool pool;

    private final AsyncQueue asyncQueue = new AsyncQueue(POOL_SIZE, "POSTGRES");

    public VertxLocalPostgresQuerySubmitServiceProvider(LocalDataSource localDataSource) {
        ConnectionDetails cd = localDataSource.getLocalConnectionDetails();
        PgConnectOptions connectOptions = new PgConnectOptions()
            .setPort(cd.getPort())
            .setHost(cd.getHost())
            .setDatabase(cd.getDatabaseName())
            .setUser(cd.getUsername())
            .setPassword(cd.getPassword());

        // Pool Options
        PoolOptions poolOptions = new PoolOptions()
            .setMaxSize(POOL_SIZE)
            .setIdleTimeout(60) // We release the connection after 1 min of inactivity (especially for remote databases)
            ;

        Supplier<Pool> poolFactory = () ->
            PgBuilder.pool()
                .with(poolOptions)
                .connectingTo(connectOptions)
                .using(VertxInstance.getVertx())
                .build();

        // Create the pool from the data object
        log("Creating pool on server start");
        pool = poolFactory.get();

        // Closing properly the poll on the server shutdown
        Shutdown.addShutdownHook(e -> {
            log("Closing pool on server shutdown");
            pool.close();
        });
    }

    @Override
    public Future<QueryResult> executeQuery(QueryArgument argument) {
        return asyncQueue.addAsyncOperation(argument, this::executeQueryNow);
    }

    private Future<QueryResult> executeQueryNow(QueryArgument argument) {
        long t0 = System.currentTimeMillis();
        return toWebfxFuture(withConnection(pool, connection -> executeConnectionQuery(connection, argument)))
            .onFailure(e -> log("⛔️ ERROR with executeQuery(" + argument + "): " + e.getMessage()))
            .onSuccess(x -> { // Just for time report
                if (LOG_TIMINGS) {
                    long executionTimeMillis = System.currentTimeMillis() - t0;
                    log((executionTimeMillis < WARNING_MILLIS ? "" : "⚠️ WARNING: ") + "Query executed in " + executionTimeMillis + "ms: " + argument);
                }
            });
    }

    @Override
    public Future<Batch<QueryResult>> executeQueryBatch(Batch<QueryArgument> batch) {
        return asyncQueue.addAsyncOperation(batch, this::executeQueryBatchNow);
    }

    private Future<Batch<QueryResult>> executeQueryBatchNow(Batch<QueryArgument> batch) {
        long t0 = System.currentTimeMillis();
        return toWebfxFuture(withConnection(pool, connection ->
            toVertxFuture(batch.executeSerial(QueryResult[]::new, arg ->
                toWebfxFuture(executeConnectionQuery(connection, arg))))
        ))
            .onFailure(e -> log("⛔️ ERROR with executeQueryBatch(" + batch + "): " + e.getMessage()))
            .onSuccess(x -> { // Just for time report
                if (LOG_TIMINGS) {
                    long executionTimeMillis = System.currentTimeMillis() - t0;
                    log((executionTimeMillis < WARNING_MILLIS ? "" : "⚠️ WARNING: ") + "Query batch executed in " + executionTimeMillis + "ms");
                }
            });
    }

    private io.vertx.core.Future<QueryResult> executeConnectionQuery(SqlConnection connection, QueryArgument argument) {
        return connection
            .preparedQuery(argument.getStatement())
            .execute(tupleFromArguments(argument.getParameters()))
            .map(VertxSqlUtil::toWebFxQueryResult);
    }

    @Override
    public Future<SubmitResult> executeSubmit(SubmitArgument argument) {
        return asyncQueue.addAsyncOperation(argument, this::executeSubmitNow);
    }

    private Future<SubmitResult> executeSubmitNow(SubmitArgument argument) {
        // Note: executeIndividualSubmitWithConnection() already logs failure and timing
        return toWebfxFuture(withConnection(pool, connection -> executeIndividualSubmitWithConnection(argument, connection, null)));
    }

    @Override
    public Future<Batch<SubmitResult>> executeSubmitBatch(Batch<SubmitArgument> batch) {
        return asyncQueue.addAsyncOperation(batch, this::executeSubmitBatchNow);
    }

    private Future<Batch<SubmitResult>> executeSubmitBatchNow(Batch<SubmitArgument> batch) {
        // This batch may use GeneratedKeyReference instances in its parameters, which we will need to resolve during
        // the execution. To do so, we create batchIndexGeneratedKeys, which is a list of generated keys for each
        // SubmitArgument in the batch (index 0 will contain the possible generated keys from the execution of the first
        // SubmitArgument, index 1 from the second, etc...)
        List<Object[]> batchIndexGeneratedKeys = new ArrayList<>(batch.getArray().length);
        long t0 = System.currentTimeMillis();
        // We embed the batch execution inside a transaction using Vert.x API (and convert the return Vert.x Future<SubmitResult> into WebFX Future<SubmitResult>)
        return toWebfxFuture(withTransaction(pool, connection ->
            // We execute the batch in a serial order (we need a couple of Vert.x <-> WebFX Future for that)
            toVertxFuture(batch.executeSerial(SubmitResult[]::new, arg -> toWebfxFuture(
                // We execute this individual submission, passing batchIndexGeneratedKeys (for GeneratedKeyReference resolution)
                executeIndividualSubmitWithConnection(arg, connection, batchIndexGeneratedKeys)
                    .map(submitResult -> { // Identity mapping, just for batchIndexGeneratedKeys management
                        // We collect the possible generated keys (if the last submission was "insert ... returning id")
                        batchIndexGeneratedKeys.add(submitResult.getGeneratedKeys());
                        return submitResult;
                    })
            )))
        )).onSuccess(x -> { // Just for time report
            if (LOG_TIMINGS) {
                long executionTimeMillis = System.currentTimeMillis() - t0;
                log((executionTimeMillis < WARNING_MILLIS ? "" : "⚠️ WARNING: ") + "Submit batch executed in " + executionTimeMillis + "ms");
            }
            onSuccessfulSubmitBatch(batch);
        });
    }

    private static void onSuccessfulSubmitBatch(Batch<SubmitArgument> batch) {
        SubmitListenerService.fireSuccessfulSubmit(batch.getArray());
    }


    private io.vertx.core.Future<SubmitResult> executeIndividualSubmitWithConnection(SubmitArgument argument, SqlConnection connection, List<Object[]> batchIndexGeneratedKeys) {
        // We get a prepared query from the connection
        PreparedQuery<RowSet<Row>> preparedQuery = connection
            .preparedQuery(argument.getStatement()); // statement can be insert, update or delete
        // We will execute the query with either a Tuple (1 row of parameters) or a List<Tuple> (several rows of parameters)
        io.vertx.core.Future<RowSet<Row>> queryExecutionFuture; // Will contain the Vert.x Future of that execution
        Object[] parameters = argument.getParameters();
        // In case there are several rows of parameters (i.e., batch of parameters)
        if (Arrays.length(parameters) == 1 && parameters[0] instanceof Batch) {
            // We get the rows of parameters from the batch. The returned array Object[] represents rows, and each row
            // contains the parameters of that row (so it's also an array of Object[])
            Object[] parametersRows = ((Batch<?>) parameters[0]).getArray();
            // For each row, we replace the GeneratedKeyReference instances with their actual generated keys (should be known as this stage)
            Arrays.forEach(parametersRows, row -> replaceGeneratedKeyReferencesWithActualGeneratedKeys((Object[]) row, batchIndexGeneratedKeys));
            // We map the row array into a Vert.x Tuple array
            Tuple[] tuples = Arrays.map(parametersRows, params -> tupleFromArguments((Object[]) params), Tuple[]::new);
            // And finally execute that batch (passing the Tuples as a list)
            queryExecutionFuture = preparedQuery.executeBatch(Arrays.asList(tuples));
        } else { // Case a single row of parameters
            // We replace the GeneratedKeyReference instances with their actual generated keys (should be known as this stage)
            replaceGeneratedKeyReferencesWithActualGeneratedKeys(parameters, batchIndexGeneratedKeys);
            // We execute that query (passing the arguments as a Vert.x Tuple)
            queryExecutionFuture = preparedQuery.execute(tupleFromArguments(parameters));
        }
        // Waiting for the completion of the previous query execution
        long t0 = System.currentTimeMillis();
        return queryExecutionFuture
            .onFailure(e -> log("⛔️ ERROR with executeIndividualSubmitWithConnection(" + argument + "): " + e.getMessage()))
            .map(rs -> { // on success, returns rs as a Vert.x RowSet<Row>
                if (LOG_TIMINGS) {
                    long executionTimeMillis = System.currentTimeMillis() - t0;
                    log((executionTimeMillis < WARNING_MILLIS ? "" : "⚠️ WARNING: ") + "Submit executed in " + executionTimeMillis + "ms: " + argument);
                }
                onSuccessfulSubmit(argument);
                // We convert that Vert.x RowSet into a WebFX SubmitResult
                return toWebFxSubmitResult(rs, argument);
            });
    }

    private static void onSuccessfulSubmit(SubmitArgument argument) {
        SubmitListenerService.fireSuccessfulSubmit(argument);
    }

    private static void replaceGeneratedKeyReferencesWithActualGeneratedKeys(Object[] parameters, List<Object[]> batchIndexGeneratedKeys) {
        if (batchIndexGeneratedKeys != null) {
            for (int i = 0, length = Arrays.length(parameters); i < length; i++) {
                Object value = parameters[i];
                if (value instanceof GeneratedKeyReference ref) {
                    // Getting the indexes (should normally refer to a previous batch already executed at this point)
                    int batchIndex = ref.getStatementBatchIndex();
                    int generatedKeyIndex = ref.getGeneratedKeyIndex();
                    // We get the actual generated key that `ref` was referring to and replace the parameter value with it
                    Object[] generatedKeys = batchIndexGeneratedKeys.get(batchIndex);
                    parameters[i] = generatedKeys[generatedKeyIndex];
                }
            }
        }
    }

    private void log(String message) {
        asyncQueue.log(message);
    }

}
