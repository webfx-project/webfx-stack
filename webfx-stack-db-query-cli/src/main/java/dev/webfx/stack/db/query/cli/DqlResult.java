package dev.webfx.stack.db.query.cli;

import dev.webfx.stack.db.query.QueryResult;

/**
 * What came back, with the two readings a probe needs and nothing else.
 *
 * <p>A count and a row set are the same thing to the bus — a {@code QueryResult} — and different things to a
 * run: an invariant is always expressed in counts, and rows are what an operator looks at when an invariant
 * has already failed. Keeping the two readings on one small type means the count never has to be inferred
 * from a row set's size, which is the mistake that turns a {@code --limit 100} into "the caller can read 100".
 *
 * @author Claude Code
 */
public record DqlResult(QueryResult queryResult) {

    /** How many rows came back. For a row probe this is bounded by {@code --limit}; see {@link #count()}. */
    public int rowCount() {
        return queryResult == null ? 0 : queryResult.getRowCount();
    }

    public int columnCount() {
        return queryResult == null ? 0 : queryResult.getColumnCount();
    }

    public String columnName(int columnIndex) {
        String[] names = queryResult == null ? null : queryResult.getColumnNames();
        return names == null || columnIndex >= names.length ? "col" + columnIndex : names[columnIndex];
    }

    public Object value(int rowIndex, int columnIndex) {
        return queryResult == null ? null : queryResult.getValue(rowIndex, columnIndex);
    }

    /**
     * The number a {@code select count(1) as n from ...} answered with.
     *
     * <p>Throws rather than guessing when the result is not one number: a count read off the wrong shape of
     * result would be a number, and every invariant in this tool compares numbers. The tool must not be able
     * to compare the wrong ones.
     *
     * <p>This is also why counts are never taken from {@link #rowCount()}: a row probe's row count is capped
     * by the limit the server was given, and reading that as a count would quietly turn "the caller can read
     * everybody" into "the caller can read 100".
     */
    public long count() {
        if (rowCount() != 1 || columnCount() != 1)
            throw new IllegalStateException("Expected one row of one column from a count statement, got "
                                            + rowCount() + " row(s) of " + columnCount() + " column(s)."
                                            + " A count probe must be written as 'select count(1) as n from ...'.");
        Object value = value(0, 0);
        if (!(value instanceof Number number))
            throw new IllegalStateException("A count statement answered with " + describe(value)
                                            + " rather than a number.");
        return number.longValue();
    }

    private static String describe(Object value) {
        return value == null ? "null" : "a " + value.getClass().getSimpleName();
    }
}
