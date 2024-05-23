package dev.webfx.stack.db.querysubmit;

import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryResultBuilder;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.impl.ArrayTuple;

/**
 * @author Bruno Salmon
 */
final class VertxSqlUtil {

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
        Object[] generatedKeys = null;
        if (submitArgument.returnGeneratedKeys() || submitArgument.getStatement().contains(" returning ")) {
            generatedKeys = new Object[rows.size()];
            int rowIndex = 0;
            for (Row row : rows) {
                generatedKeys[rowIndex++] = row.getValue(0);
            }
        }
        return new SubmitResult(rows.rowCount(), generatedKeys);
    }

    static Tuple tupleFromArguments(Object[] parameters) {
        if (parameters == null)
            return new ArrayTuple(0);
        return Tuple.from(parameters);
    }
}
