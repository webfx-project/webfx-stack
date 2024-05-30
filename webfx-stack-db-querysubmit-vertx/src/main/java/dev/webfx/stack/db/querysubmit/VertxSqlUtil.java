package dev.webfx.stack.db.querysubmit;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryResultBuilder;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.impl.ArrayTuple;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Bruno Salmon
 */
final class VertxSqlUtil {

    private static final int MAX_RETRY_COUNT = 20;

    static QueryResult toWebFxQueryResult(RowSet<Row> rs) {
        int columnCount = rs.columnsNames().size();
        int rowCount = rs.size();
        QueryResultBuilder rsb = QueryResultBuilder.create(rowCount, columnCount);
        // deactivated column names serialization - rsb.setColumnNames(rs.getColumnNames().toArray(new String[columnCount]));
        int rowIndex = 0;
        for (Row row : rs) {
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                Object value = row.getValue(columnIndex);
                rsb.setValue(rowIndex, columnIndex, value);
            }
            rowIndex++;
        }
        // Console.log("Sql executed in " + (System.currentTimeMillis() - t0) + " ms: " + queryArgument);
        // Building and returning the final QueryResult
        return rsb.build();
    }

    static SubmitResult toWebFxSubmitResult(RowSet<Row> rows, SubmitArgument submitArgument) {
        int rowCount = 0;
        List<Object> generatedKeys = null;
        if (submitArgument.returnGeneratedKeys() || submitArgument.getStatement().contains(" returning "))
            generatedKeys = new ArrayList<>();
        for (; rows != null; rows = rows.next(), rowCount++) {
            if (generatedKeys != null) {
                Row row = rows.iterator().next();
                generatedKeys.add(row.getValue(0));
            }
        }
        return new SubmitResult(rowCount, generatedKeys == null ? null : generatedKeys.toArray());
    }

    static Tuple tupleFromArguments(Object[] parameters) {
        if (parameters == null)
            return new ArrayTuple(0);
        return Tuple.from(parameters);
    }

    static <T> Future<T> withConnection(Pool pool, Function<SqlConnection, Future<T>> function) {
        //return pool.withConnection(function); // The issue is that it always returns the connection to the pool even if it's broken
        return tryAndRetryOnBrokenConnection(0, () -> pool.getConnection()
                .compose( connection -> function
                        .apply(connection)
                        .onComplete(ar -> returnConnectionToPoolIfNotBroken(ar, connection)))
        );
    }

    static <T> Future<T> withTransaction(Pool pool, Function<SqlConnection, Future<T>> function) {
        //return pool.withTransaction(function); // The issue is that it always returns the connection to the pool even if it's broken
        return tryAndRetryOnBrokenConnection(0, () -> pool.getConnection()
                .flatMap(conn -> conn
                        .begin()
                        .flatMap(tx -> function
                                .apply(conn)
                                .compose(
                                        res -> tx
                                                .commit()
                                                .flatMap(v -> Future.succeededFuture(res)),
                                        err -> {
                                            if (err instanceof TransactionRollbackException) {
                                                return Future.failedFuture(err);
                                            } else {
                                                return tx
                                                        .rollback()
                                                        .compose(v -> Future.failedFuture(err), failure -> Future.failedFuture(err));
                                            }
                                        }))
                        //.onComplete(ar -> conn.close()));
                        .onComplete(ar -> returnConnectionToPoolIfNotBroken(ar, conn)))
        );
    }

    private static <T> void returnConnectionToPoolIfNotBroken(AsyncResult<T> ar, SqlConnection connection) {
        // Returning to the pool, unless it's broken (i.e. SocketException, typically "Connection reset")
        if (!isBrokenConnectionCause(ar.cause()))
            connection.close();
    }

    private static boolean isBrokenConnectionCause(Throwable cause) {
        // We consider the connection broken when we get a SocketException (typically "Connection reset")
        return cause instanceof SocketException;
    }

    private static <T> Future<T> tryAndRetryOnBrokenConnection(int retryCount, Supplier<Future<T>> connectionMethod) {
        return connectionMethod.get()
                .recover(cause -> {
                    if (isBrokenConnectionCause(cause) && retryCount <= MAX_RETRY_COUNT) {
                        Console.log("Detected broken database connection, retrying with another connection (retryCount = " + (retryCount + 1) + ")");
                        return tryAndRetryOnBrokenConnection(retryCount + 1, connectionMethod);
                    }
                    return Future.failedFuture(cause);
                });
    }
}
