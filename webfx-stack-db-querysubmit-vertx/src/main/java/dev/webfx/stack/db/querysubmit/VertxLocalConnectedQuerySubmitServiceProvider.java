package dev.webfx.stack.db.querysubmit;

import dev.webfx.stack.db.submit.listener.SubmitListenerService;
import dev.webfx.stack.db.datasource.ConnectionDetails;
import dev.webfx.stack.db.datasource.DBMS;
import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.datasource.jdbc.JdbcDriverInfo;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryResultBuilder;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.db.submit.GeneratedKeyBatchIndex;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;
import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.util.tuples.Unit;
import dev.webfx.platform.vertx.common.VertxInstance;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author Bruno Salmon
 */
public final class VertxLocalConnectedQuerySubmitServiceProvider implements QueryServiceProvider, SubmitServiceProvider {

    private final static boolean LOG_OPEN_CONNECTIONS = true;

    private final Pool pool;

    public VertxLocalConnectedQuerySubmitServiceProvider(LocalDataSource localDataSource) {
        // Generating the Vertx Sql config from the connection details
        ConnectionDetails connectionDetails = localDataSource.getLocalConnectionDetails();
        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(20);
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
                pool = PgPool.pool(vertx, connectOptions, poolOptions);
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
        return connectAndExecuteInTransaction((connection, transaction, batchPromise) -> executeUpdateBatchOnConnection(batch, connection, transaction, batchPromise));
    }


    // ==================================== PRIVATE IMPLEMENTATION PART  ===============================================

    private <T> Future<T> connectAndExecute(BiConsumer<SqlConnection, Promise<T>> executor) {
        Promise<T> promise = Promise.promise();
        pool.getConnection()
                .onFailure(cause -> {
                    Console.log(cause);
                    promise.fail(cause);
                })
                .onSuccess(connection -> {
                    if (LOG_OPEN_CONNECTIONS)
                        Console.log("DB pool open connections = " + ++open);
                    executor.accept(connection, promise);
                }); // Note: this is the responsibility of the executor to close the connection
        return promise.future();
    }

    private interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    private <T> Future<T> connectAndExecuteInTransaction(TriConsumer<SqlConnection, Transaction, Promise<T>> executor) {
        return connectAndExecute((connection, promise) ->
                connection.begin()
                        .onFailure(cause -> { Console.log(cause); promise.fail(cause); })
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
                Console.log(ar.cause());
                promise.fail(ar.cause());
            } else { // Sql succeeded
                // Transforming the result set into columnNames and values arrays
                RowSet<Row> resultSet = ar.result();
                int columnCount = resultSet.columnsNames().size();
                int rowCount = resultSet.size();
                QueryResultBuilder rsb = QueryResultBuilder.create(rowCount, columnCount);
                // deactivated column names serialization - rsb.setColumnNames(resultSet.getColumnNames().toArray(new String[columnCount]));
                int rowIndex = 0;
                for (Row row : resultSet) {
                    for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                        Object value = row.getValue(columnIndex);
                        if (value instanceof LocalDate)
                            value = ((LocalDate) value).atStartOfDay().toInstant(ZoneOffset.UTC);
                        else if (value instanceof LocalDateTime)
                            value = ((LocalDateTime) value).toInstant(ZoneOffset.UTC);
                        rsb.setValue(rowIndex, columnIndex, value);
                    }
                    rowIndex++;
                }
                // Logger.log("Sql executed in " + (System.currentTimeMillis() - t0) + " ms: " + queryArgument);
                // Building and returning the final QueryResult
                promise.complete(rsb.build());
            }
            // Closing the connection, so it can go back to the pool
            closeConnection(connection);
        });
    }

    private void executeQueryOnConnection(String queryString, Object[] parameters, SqlConnection connection, Handler<AsyncResult<RowSet<Row>>> resultHandler) {
        // Calling query() or preparedQuery() depending on if parameters are provided or not
        if (Arrays.isEmpty(parameters)) {
            connection.query(queryString)
                    .execute(resultHandler);
        } else {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] instanceof Instant)
                    parameters[i] = LocalDateTime.ofInstant((Instant) parameters[i], ZoneOffset.UTC);
            }
            connection.preparedQuery(queryString)
                    .execute(Tuple.from(parameters), resultHandler);
        }
    }

    private Future<SubmitResult> executeSubmitOnConnection(SubmitArgument submitArgument, SqlConnection connection, Transaction transaction, boolean batch, Promise<SubmitResult> promise) {
        Console.log(submitArgument);
        executeQueryOnConnection(submitArgument.getStatement(), submitArgument.getParameters(), connection, res -> {
            if (res.failed()) { // Sql error
                // Unless from batch, closing the connection now, so it can go back to the pool
                if (!batch)
                    closeConnection(connection);
                Console.log(res.cause());
                promise.fail(res.cause());
            } else { // Sql succeeded
                RowSet<Row> result = res.result();
                Object[] generatedKeys = null;
                if (submitArgument.returnGeneratedKeys() || submitArgument.getStatement().contains(" returning ")) {
                    generatedKeys = new Object[result.size()];
                    int rowIndex = 0;
                    for (Row row : result)
                        generatedKeys[rowIndex++] = row.getValue(0);
                }
                SubmitResult submitResult = new SubmitResult(result.rowCount(), generatedKeys);
                if (batch)
                    promise.complete(submitResult);
                else {
                    transaction.commit(ar -> {
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

    private void executeUpdateBatchOnConnection(Batch<SubmitArgument> batch, SqlConnection connection, Transaction transaction, Promise<Batch<SubmitResult>> batchPromise) {
        List<Object> batchIndexGeneratedKeys = new ArrayList<>(Collections.nCopies(batch.getArray().length, null));
        Unit<Integer> batchIndex = new Unit<>(0);
        batch.executeSerial(batchPromise, SubmitResult[]::new, updateArgument -> {
            Promise<SubmitResult> statementPromise = Promise.promise();
            // Replacing GeneratedKeyBatchIndex parameters with their actual generated keys
            Object[] parameters = updateArgument.getParameters();
            for (int i = 0, length = Arrays.length(parameters); i < length; i++) {
                Object value = parameters[i];
                if (value instanceof GeneratedKeyBatchIndex)
                    parameters[i] = batchIndexGeneratedKeys.get(((GeneratedKeyBatchIndex) value).getBatchIndex());
            }
            executeSubmitOnConnection(updateArgument, connection, transaction, true, Promise.promise())
                    .onFailure(cause -> {
                        Console.log(cause);
                        statementPromise.fail(cause);
                        transaction.rollback(event -> closeConnection(connection));
                    })
                    .onSuccess(submitResult -> {
                        Object[] generatedKeys = submitResult.getGeneratedKeys();
                        if (!Arrays.isEmpty(generatedKeys))
                            batchIndexGeneratedKeys.set(batchIndex.get(), generatedKeys[0]);
                        batchIndex.set(batchIndex.get() + 1);
                        if (batchIndex.get() < batch.getArray().length)
                            statementPromise.complete(submitResult);
                        else
                            transaction.commit(ar -> {
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
