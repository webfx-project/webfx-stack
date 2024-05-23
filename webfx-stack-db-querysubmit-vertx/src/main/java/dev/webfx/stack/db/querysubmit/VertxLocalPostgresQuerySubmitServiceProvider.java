package dev.webfx.stack.db.querysubmit;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
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
import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.*;

import java.util.ArrayList;
import java.util.List;

import static dev.webfx.platform.vertx.common.VertxFutureUtil.toVertxFuture;
import static dev.webfx.platform.vertx.common.VertxFutureUtil.toWebFxFuture;

/**
 * @author Bruno Salmon
 */
public class VertxLocalPostgresQuerySubmitServiceProvider implements QueryServiceProvider, SubmitServiceProvider {

    private final Pool pool;

    public VertxLocalPostgresQuerySubmitServiceProvider(LocalDataSource localDataSource) {
        ConnectionDetails cd = localDataSource.getLocalConnectionDetails();
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(cd.getPort())
                .setHost(cd.getHost())
                .setDatabase(cd.getDatabaseName())
                .setUser(cd.getUsername())
                .setPassword(cd.getPassword());

        // Pool Options
        PoolOptions poolOptions = new PoolOptions().setMaxSize(10);

        // Create the pool from the data object
        pool = PgBuilder.pool()
                .with(poolOptions)
                .connectingTo(connectOptions)
                .using(VertxInstance.getVertx())
                .build();
    }

    @Override
    public Future<QueryResult> executeQuery(QueryArgument argument) {
        return toWebFxFuture( pool.withConnection(connection -> executeConnectionQuery(connection, argument)) );
    }

    @Override
    public Future<Batch<QueryResult>> executeQueryBatch(Batch<QueryArgument> batch) {
        return toWebFxFuture( pool.withConnection(connection ->
            toVertxFuture( batch.executeSerial(QueryResult[]::new, arg ->
                    toWebFxFuture( executeConnectionQuery(connection, arg))))
        ));
    }

    private io.vertx.core.Future<QueryResult> executeConnectionQuery(SqlConnection connection, QueryArgument argument) {
        return connection
                .preparedQuery(argument.getStatement())
                .execute(VertxSqlUtil.tupleFromArguments(argument.getParameters()))
                .map(VertxSqlUtil::toWebFxQueryResult);
    }

    @Override
    public Future<SubmitResult> executeSubmit(SubmitArgument argument) {
        return toWebFxFuture( pool.withConnection(connection -> executeIndividualSubmitWithConnection(argument, connection, null)) );
    }

    @Override
    public Future<Batch<SubmitResult>> executeSubmitBatch(Batch<SubmitArgument> batch) {
        // This batch may use GeneratedKeyReference instances in its parameters, which we will need to resolve during
        // the execution. To do so, we create batchIndexGeneratedKeys which is a list of generated keys for each
        // individual SubmitArgument in the batch (index 0 will contain the possible generated keys from the execution
        // of the first SubmitArgument, index 1 from the second, etc...)
        List<Object[]> batchIndexGeneratedKeys = new ArrayList<>(batch.getArray().length);
        long t0 = System.currentTimeMillis();
        // We embed the batch execution inside a transaction using Vert.x API (and convert the return Vert.x Future<SubmitResult> into WebFX Future<SubmitResult>)
        return toWebFxFuture(pool.withTransaction(connection ->
            // We execute the batch in a serial order (we need a couple of Vert.x <-> WebFX Future for that)
            toVertxFuture( batch.executeSerial(SubmitResult[]::new, arg -> toWebFxFuture(
                    // We execute each individual submit, passing batchIndexGeneratedKeys (for GeneratedKeyReference resolution)
                    executeIndividualSubmitWithConnection(arg, connection, batchIndexGeneratedKeys)
                            .map(submitResult -> { // Identity mapping, just for batchIndexGeneratedKeys management
                                // We collect the possible generated keys (if the last submit was "insert ... returning id")
                                batchIndexGeneratedKeys.add(submitResult.getGeneratedKeys());
                                return submitResult;
                            })
                    )))
        )).map(x -> { // Identity mapping just for time report
            long t1 = System.currentTimeMillis();
            Console.log("DB query batch executed in " + (t1 - t0) + "ms");
            return x;
        });
    }

    private io.vertx.core.Future<SubmitResult> executeIndividualSubmitWithConnection(SubmitArgument argument, SqlConnection connection, List<Object[]> batchIndexGeneratedKeys) {
        // We get a prepared query from the connection
        PreparedQuery<RowSet<Row>> preparedQuery = connection
                .preparedQuery(argument.getStatement()); // statement can be insert, update or delete
        // We will execute the query with either a Tuple (1 row of parameters), or a List<Tuple> (several rows of parameters)
        io.vertx.core.Future<RowSet<Row>> queryExecutionFuture; // Will contain the Vert.x Future of that execution
        Object[] parameters = argument.getParameters();
        // Case of several rows of parameters (i.e. batch of parameters)
        if (Arrays.length(parameters) == 1 && parameters[0] instanceof Batch) {
            // We get the rows of parameters from the batch. The returned array Object[] represents rows, and each row
            // contains the parameters of that row (so it's also an array of Object[])
            Object[] parametersRows = ((Batch) parameters[0]).getArray();
            // For each row, we replace the GeneratedKeyReference instances with their actual generated keys (should be known as this stage)
            Arrays.forEach(parametersRows, row -> replaceGeneratedKeyReferenceParameters((Object[]) row, batchIndexGeneratedKeys));
            // We map the rows array into a Vert.x Tuple array
            Tuple[] tuples = Arrays.map(parametersRows, params -> VertxSqlUtil.tupleFromArguments((Object[]) params), Tuple[]::new);
            // And finally execute that batch (passing the Tuples as a list)
            queryExecutionFuture = preparedQuery.executeBatch(Arrays.asList(tuples));
        } else { // Case a single row of parameters
            // We replace the GeneratedKeyReference instances with their actual generated keys (should be known as this stage)
            replaceGeneratedKeyReferenceParameters(parameters, batchIndexGeneratedKeys);
            // We execute that query (passing the arguments as a Vert.x Tuple)
            queryExecutionFuture = preparedQuery.execute(VertxSqlUtil.tupleFromArguments(parameters));
        }
        // Waiting the completion of the previous query execution
        long t0 = System.currentTimeMillis();
        return queryExecutionFuture
                .map(rs -> { // on success, returns rs as a Vert.x RowSet<Row>
                    long t1 = System.currentTimeMillis();
                    Console.log("DB query executed in " + (t1 - t0) + "ms (" + argument.getStatement() + ")");
                    // We convert that Vert.x RowSet into a WebFX SubmitResult
                    return VertxSqlUtil.toWebFxSubmitResult(rs, argument);
                });
    }

    private static void replaceGeneratedKeyReferenceParameters(Object[] parameters, List<Object[]> batchIndexGeneratedKeys) {
        if (batchIndexGeneratedKeys != null) {
            for (int i = 0, length = Arrays.length(parameters); i < length; i++) {
                Object value = parameters[i];
                if (value instanceof GeneratedKeyReference) {
                    GeneratedKeyReference generatedKeyReference = (GeneratedKeyReference) value;
                    Object[] generatedKeys = batchIndexGeneratedKeys.get(generatedKeyReference.getStatementBatchIndex());
                    parameters[i] = generatedKeys[generatedKeyReference.getGeneratedKeyIndex()];
                }
            }
        }
    }

}
