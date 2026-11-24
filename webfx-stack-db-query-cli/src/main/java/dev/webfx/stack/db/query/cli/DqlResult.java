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

    /** The alias every probe's count statement is written with, and the only way the count column is found. */
    public static final String COUNT_ALIAS = "n";

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
        if (rowCount() != 1)
            throw new IllegalStateException("Expected one row from a count statement, got " + rowCount()
                                            + ". A count probe must be written as 'select count(1) as "
                                            + COUNT_ALIAS + " from ...'.");
        int columnIndex = countColumnIndex();
        Object value = value(0, columnIndex);
        if (!(value instanceof Number number))
            throw new IllegalStateException("A count statement answered with " + describe(value)
                                            + " rather than a number.");
        return number.longValue();
    }

    /**
     * Which column holds the count — the LAST one, and the reason is worth the paragraph.
     *
     * <p>The first real run of this tool got {@code 1 row(s) of 2 column(s)} back from
     * {@code select count(1) as n from Conversation where event=$1}. Asking the server for the rows showed
     * what they are:
     *
     * <pre>
     *   col0  col1
     *   ----  ----
     *   1     133
     * </pre>
     *
     * <p>Modality's DQL prepends a column of its own to an entity query — the entity's id, which for an
     * aggregate degenerates to a constant — and the select list follows it. So <b>position 0 is not the
     * count</b>, and a reader that assumed it was would have returned 1 for every probe: a number, plausible,
     * and silently wrong, which is the single failure this class exists to prevent.
     *
     * <p>It is read by position rather than by the {@code as n} alias because <b>the result carries no column
     * names at all</b> over this path — {@code getColumnNames()} is null, which is why the table above is
     * headed {@code col0 col1}. An alias-based reader was written first and refused every probe.
     *
     * <p>What makes "the last column" safe rather than lucky: every probe's statement is pinned by
     * {@code ProbeCatalogueCheck} to {@code select count(1) as n from …} — exactly one item in the select
     * list — so the last column IS that item. A statement with two selected values is not a count probe, and
     * three columns is refused here rather than guessed at.
     *
     * <p>Corroboration that this reads the right column: 133 is what
     * {@code scripts/predict-chat-read-scope.sql} independently predicts for event 1857 from the grant tables.
     * Two instruments, one number.
     */
    private int countColumnIndex() {
        int columnCount = columnCount();
        if (columnCount < 1 || columnCount > 2)
            throw new IllegalStateException("Expected one or two columns from a count statement (Modality DQL"
                                            + " prepends one of its own to the select list), got " + columnCount
                                            + ". A count probe must be written as 'select count(1) as "
                                            + COUNT_ALIAS + " from ...'.");
        return columnCount - 1;
    }

    private static String describe(Object value) {
        return value == null ? "null" : "a " + value.getClass().getSimpleName();
    }
}
