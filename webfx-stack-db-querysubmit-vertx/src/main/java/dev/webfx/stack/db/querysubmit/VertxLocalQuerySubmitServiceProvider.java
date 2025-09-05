package dev.webfx.stack.db.querysubmit;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.util.tuples.Unit;
import dev.webfx.platform.util.vertx.VertxInstance;
import dev.webfx.stack.db.datasource.ConnectionDetails;
import dev.webfx.stack.db.datasource.DBMS;
import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.datasource.jdbc.JdbcDriverInfo;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.db.submit.GeneratedKeyReference;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.listener.SubmitListenerService;
import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.*;

import java.net.SocketException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author Bruno Salmon
 */
public final class VertxLocalQuerySubmitServiceProvider implements QueryServiceProvider, SubmitServiceProvider {

    private final static boolean LOG_OPEN_CONNECTIONS = false;

    private final Pool pool;

    public VertxLocalQuerySubmitServiceProvider(LocalDataSource localDataSource) {
        // Generating the Vertx SQL config from the connection details
        ConnectionDetails connectionDetails = localDataSource.getLocalConnectionDetails();
        PoolOptions poolOptions = new PoolOptions()
            .setMaxSize(10);
        Vertx vertx = VertxInstance.getVertx();
        DBMS dbms = localDataSource.getDBMS();
        switch (dbms) {
            case POSTGRES: {
                PgConnectOptions connectOptions = new PgConnectOptions()
                    .setHost(connectionDetails.getHost())
                    .setPort(connectionDetails.getPort())
                    .setDatabase(connectionDetails.getDatabaseName())
                    .setUser(connectionDetails.getUsername())
                    .setPassword(connectionDetails.getPassword());
                pool = PgBuilder.pool()
                    .with(poolOptions)
                    .connectingTo(connectOptions)
                    .using(VertxInstance.getVertx())
                    .build();
                break;
            }
            case MYSQL: // TODO implement MySQL
            default: {
                JdbcDriverInfo jdbcDriverInfo = JdbcDriverInfo.from(dbms);
                JDBCConnectOptions connectOptions = new JDBCConnectOptions()
                    .setJdbcUrl(jdbcDriverInfo.getUrlOrGenerateJdbcUrl(connectionDetails))
                    .setDatabase(connectionDetails.getDatabaseName()) // Necessary?
                    .setUser(connectionDetails.getUsername())
                    .setPassword(connectionDetails.getPassword());
                // Note: Works only with the Agroal connection pool
                pool = JDBCPool.pool(vertx, connectOptions, poolOptions);
            }
        }
        // Adding a shutdown hook to close the pool on server shutdown (or should we use WebFX boot API?)
        Runtime.getRuntime().addShutdownHook(new Thread(pool::close));
    }

    @Override
    public Future<QueryResult> executeQuery(QueryArgument queryArgument) {
        return connectAndExecute((connection, future) -> executeSingleQueryOnConnection(queryArgument, connection, future));
    }

    @Override
    public Future<SubmitResult> executeSubmit(SubmitArgument submitArgument) {
        return connectAndExecuteInTransaction((connection, transaction, future) -> executeSubmitOnConnection(submitArgument, connection, transaction, false, future));
    }

    @Override
    public Future<Batch<SubmitResult>> executeSubmitBatch(Batch<SubmitArgument> batch) {
        // Singular batch optimization: executing the single sql order in autocommit mode
        Future<Batch<SubmitResult>> singularBatchFuture = batch.executeIfSingularBatch(SubmitResult[]::new, this::executeSubmit);
        if (singularBatchFuture != null)
            return singularBatchFuture;

        // Now handling real batch with several arguments -> no autocommit with explicit commit() or rollback() handling
        return connectAndExecuteInTransaction((connection, transaction, batchPromise) -> executeSubmitBatchOnConnection(batch, connection, transaction, batchPromise));
    }


    // ==================================== PRIVATE IMPLEMENTATION PART  ===============================================

    private <T> Future<T> connectAndExecute(BiConsumer<SqlConnection, Promise<T>> executor) {
        Promise<T> promise = Promise.promise();
        pool.getConnection()
            .onFailure(cause -> {
                Console.log("DB connectAndExecute() failed", cause);
                promise.fail(cause);
            })
            .onSuccess(connection -> { // Note: this is the responsibility of the executor to close the connection
                Promise<T> intermediatePromise = Promise.promise(); // used to check SocketException
                if (LOG_OPEN_CONNECTIONS)
                    Console.log("DB pool open connections = " + ++open);
                executor.accept(connection, intermediatePromise);
                intermediatePromise.future()
                    .onSuccess(promise::complete)
                    .onFailure(cause -> {
                        if (!(cause instanceof SocketException)) {
                            promise.fail(cause);
                        } else {
                            // We retry with another connection from the pool
                            Console.log("Retrying with another connection from the pool");
                            connectAndExecute(executor) // trying again (another loop may happen if several connections are broken)
                                // Once complete (including the possible subsequent loops),
                                .onComplete(promise); // we transfer back the final result.
                        }
                    });
            });
        return promise.future();
    }

    private interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    private <T> Future<T> connectAndExecuteInTransaction(TriConsumer<SqlConnection, Transaction, Promise<T>> executor) {
        return connectAndExecute((connection, promise) ->
            connection.begin()
                .onFailure(cause -> {
                    Console.log("DB connectAndExecuteInTransaction() failed", cause);
                    promise.fail(cause);
                })
                .onSuccess(transaction -> executor.accept(connection, transaction, promise))
        );
    }

    private int open;

    private void closeConnection(SqlConnection connection) {
        connection.close();
        if (LOG_OPEN_CONNECTIONS)
            Console.log("DB pool open connections = " + --open);
    }

    private static void onSuccessfulSubmit(SubmitArgument submitArgument) {
        SubmitListenerService.fireSuccessfulSubmit(submitArgument);
    }

    private static void onSuccessfulSubmitBatch(Batch<SubmitArgument> batch) {
        SubmitListenerService.fireSuccessfulSubmit(batch.getArray());
    }

    private void executeSingleQueryOnConnection(QueryArgument queryArgument, SqlConnection connection, Promise<QueryResult> promise) {
        //Console.log("Single query with " + queryArgument);
        // long t0 = System.currentTimeMillis();
        executeQueryOnConnection(queryArgument.getStatement(), queryArgument.getParameters(), connection, ar -> {
            if (ar.failed()) { // Sql error
                Console.log("DB executeSingleQueryOnConnection() failed when executing: " + queryArgument.getStatement(), ar.cause());
                promise.fail(ar.cause());
            } else { // Sql succeeded
                // Transforming the result set into columnNames and values arrays
                QueryResult rs = VertxSqlUtil.toWebFxQueryResult(ar.result());
                promise.complete(rs);
            }
            // Closing the connection, so it can go back to the pool
            closeConnection(connection);
        });
    }

    private void executeQueryOnConnection(String queryString, Object[] parameters, SqlConnection connection, Handler<AsyncResult<RowSet<Row>>> resultHandler) {
        // Calling query() or preparedQuery() depending on if parameters are provided or not
        if (Arrays.isEmpty(parameters)) {
            connection.query(queryString)
                .execute()
                .onComplete(resultHandler);
        } else {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] instanceof Instant)
                    parameters[i] = LocalDateTime.ofInstant((Instant) parameters[i], ZoneOffset.UTC);
            }
            connection.preparedQuery(queryString)
                .execute(Tuple.from(parameters))
                .onComplete(resultHandler);
        }
    }

    private Future<SubmitResult> executeSubmitOnConnection(SubmitArgument submitArgument, SqlConnection connection, Transaction transaction, boolean batch, Promise<SubmitResult> promise) {
        Console.log(submitArgument);
        executeQueryOnConnection(submitArgument.getStatement(), submitArgument.getParameters(), connection, res -> {
            if (res.failed()) { // Sql error
                // Unless from batch, closing the connection now, so it can go back to the pool
                if (!batch)
                    closeConnection(connection);
                Console.log("DB executeSubmitOnConnection() failed", res.cause());
                promise.fail(res.cause());
            } else { // Sql succeeded
                SubmitResult submitResult = VertxSqlUtil.toWebFxSubmitResult(res.result(), submitArgument);
                if (batch)
                    promise.complete(submitResult);
                else {
                    transaction.commit()
                        .onComplete(ar -> {
                            if (ar.failed()) {
                                Console.log(ar.cause());
                                promise.fail(ar.cause());
                            } else
                                promise.complete(submitResult);
                            closeConnection(connection);
                            if (ar.succeeded())
                                onSuccessfulSubmit(submitArgument);
                        });
                }
            }
        });
        return promise.future();
    }

    private void executeSubmitBatchOnConnection(Batch<SubmitArgument> batch, SqlConnection connection, Transaction transaction, Promise<Batch<SubmitResult>> batchPromise) {
        List<Object> batchIndexGeneratedKeys = new ArrayList<>(Collections.nCopies(batch.getArray().length, null));
        Unit<Integer> batchIndex = new Unit<>(0);
        batch.executeSerial(batchPromise, SubmitResult[]::new, updateArgument -> {
            Promise<SubmitResult> statementPromise = Promise.promise();
            // Replacing GeneratedKeyBatchIndex parameters with their actual generated keys
            Object[] parameters = updateArgument.getParameters();
            for (int i = 0, length = Arrays.length(parameters); i < length; i++) {
                Object value = parameters[i];
                if (value instanceof GeneratedKeyReference)
                    parameters[i] = batchIndexGeneratedKeys.get(((GeneratedKeyReference) value).getStatementBatchIndex());
            }
            long t0 = System.currentTimeMillis();
            executeSubmitOnConnection(updateArgument, connection, transaction, true, Promise.promise())
                .onFailure(cause -> {
                    Console.log("DB executeUpdateBatchOnConnection()", cause);
                    statementPromise.fail(cause);
                    transaction.rollback().onComplete(ar -> closeConnection(connection));
                })
                .onSuccess(submitResult -> {
                    long t1 = System.currentTimeMillis();
                    Console.log("DB submit batch executed in " + (t1 - t0) + "ms");
                    Object[] generatedKeys = submitResult.getGeneratedKeys();
                    if (!Arrays.isEmpty(generatedKeys))
                        batchIndexGeneratedKeys.set(batchIndex.get(), generatedKeys[0]);
                    batchIndex.set(batchIndex.get() + 1);
                    if (batchIndex.get() < batch.getArray().length)
                        statementPromise.complete(submitResult);
                    else
                        transaction.commit()
                            .onComplete(ar -> {
                                if (ar.failed()) {
                                    Console.log(ar.cause());
                                    statementPromise.fail(ar.cause());
                                } else
                                    statementPromise.complete(submitResult);
                                closeConnection(connection);
                                if (ar.succeeded())
                                    onSuccessfulSubmitBatch(batch);
                            });
                });
            return statementPromise.future();
        });
    }
}
