package dev.webfx.stack.db.querysubmit;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryResultBuilder;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.sqlclient.*;

import java.net.SocketException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Bruno Salmon
 */
final class VertxSqlUtil {

    private static final int MAX_RETRY_COUNT = 20;

    static Tuple tupleFromArguments(Object[] parameters) {
        if (parameters == null)
            return Tuple.tuple();
        // Converting parameters not directly supported by the Vert.x PostgreSQL client:
        // - Instant => OffsetDateTime
        // - Object[] => typed array (Integer[], String[], etc.) — see toTypedArrayParameter()
        Object[] postgresParameters = parameters;
        for (int i = 0; i < parameters.length; i++) {
            Object parameter = parameters[i];
            if (parameter instanceof Instant instant)
                parameter = instant.atOffset(ZoneOffset.UTC);
            else if (parameter instanceof Object[] array)
                parameter = toTypedArrayParameter(array);
            if (parameter != parameters[i] && postgresParameters == parameters)
                postgresParameters = parameters.clone();
            if (postgresParameters != parameters)
                postgresParameters[i] = parameter;
        }
        return Tuple.from(postgresParameters);
    }

    /**
     * Converts an untyped Object[] parameter into a typed Java array. A JSON array parameter (the DQL array
     * membership form `x in $n`, compiled to SQL `x = any($n)`) arrives over the bus as Object[], but the
     * Vert.x PostgreSQL client maps Java array CLASSES to Postgres array types (Integer[] => int4[], etc.),
     * so an untyped Object[] can't be bound.
     */
    private static Object toTypedArrayParameter(Object[] array) {
        int n = array.length;
        boolean allInteger = true, allIntegral = true, allNumber = true, allString = true, allLocalDate = true;
        for (Object e : array) {
            if (e == null)
                continue; // null elements are valid in any typed array
            allInteger   &= e instanceof Integer;
            allIntegral  &= e instanceof Integer || e instanceof Long;
            allNumber    &= e instanceof Number;
            allString    &= e instanceof String;
            allLocalDate &= e instanceof LocalDate;
        }
        if (allInteger) { // also the empty-array case => int4[] (ids being the dominant use of array parameters)
            Integer[] typed = new Integer[n];
            for (int i = 0; i < n; i++)
                typed[i] = (Integer) array[i];
            return typed;
        }
        if (allIntegral) {
            Long[] typed = new Long[n];
            for (int i = 0; i < n; i++)
                typed[i] = array[i] == null ? null : ((Number) array[i]).longValue();
            return typed;
        }
        if (allNumber) {
            Double[] typed = new Double[n];
            for (int i = 0; i < n; i++)
                typed[i] = array[i] == null ? null : ((Number) array[i]).doubleValue();
            return typed;
        }
        if (allString) {
            String[] typed = new String[n];
            for (int i = 0; i < n; i++)
                typed[i] = (String) array[i];
            return typed;
        }
        if (allLocalDate) {
            LocalDate[] typed = new LocalDate[n];
            for (int i = 0; i < n; i++)
                typed[i] = (LocalDate) array[i];
            return typed;
        }
        return array; // unsupported element mix — let the Vert.x client raise its explicit encoding error
    }

    static QueryResult toWebFxQueryResult(RowSet<Row> rs) {
        int columnCount = rs.columnsNames().size();
        int rowCount = rs.size();
        QueryResultBuilder rsb = QueryResultBuilder.create(rowCount, columnCount);
        // deactivated column names serialization - rsb.setColumnNames(rs.getColumnNames().toArray(new String[columnCount]));
        int rowIndex = 0;
        for (Row row : rs) {
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                Object value = row.getValue(columnIndex);
                // Converting all OffsetDateTime UTC to Instant
                if (value instanceof OffsetDateTime offsetDateTime && ZoneOffset.UTC.equals(offsetDateTime.getOffset())) {
                    value = offsetDateTime.toInstant();
                }
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
                // The " returning " detection above is a substring match, so it can misfire on
                // statements that merely CONTAIN the word without returning rows (e.g. a CREATE
                // FUNCTION whose PL/pgSQL body has an INSERT ... RETURNING — found by the V0003
                // boot migration), and a genuine RETURNING can also affect zero rows. An empty
                // row set must contribute no key rather than throw NoSuchElementException.
                var iterator = rows.iterator();
                if (iterator.hasNext())
                    generatedKeys.add(iterator.next().getValue(0));
            }
        }
        return new SubmitResult(rowCount, generatedKeys == null ? null : generatedKeys.toArray());
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
