package dev.webfx.stack.db.query.cli;

import java.io.PrintStream;

/**
 * Prints a result set as a plain table, and nothing when it was not asked to.
 *
 * <p>Separate from the runner because showing rows is a decision, not a formatting detail. A count answers
 * every invariant this tool evaluates; rows are for the moment an invariant has already failed and somebody
 * needs to see which rows. The tool's default is therefore counts, and the caller opts in to the rest.
 *
 * @author Claude Code
 */
public final class ResultPrinter {

    private ResultPrinter() {}

    /** A count, on its own line, with the statement's own column name. */
    public static void printCount(PrintStream out, String label, long count) {
        out.println(label + ": " + count);
    }

    /**
     * The rows, as a table.
     *
     * <p>Values are printed with {@code String.valueOf}, which is right for a tool whose job is to show what
     * the server actually answered: a formatter that tidied nulls, trimmed strings or localised dates would be
     * deciding what the result looks like, and the result is the evidence.
     */
    public static void printRows(PrintStream out, DqlResult result, int offset) {
        int columns = result.columnCount();
        int rows = result.rowCount();
        if (rows == 0) {
            out.println("(no rows)");
            return;
        }
        String[] headers = new String[columns];
        int[] widths = new int[columns];
        for (int c = 0; c < columns; c++) {
            headers[c] = result.columnName(c);
            widths[c] = headers[c].length();
        }
        String[][] cells = new String[rows][columns];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < columns; c++) {
                cells[r][c] = String.valueOf(result.value(r, c));
                widths[c] = Math.max(widths[c], cells[r][c].length());
            }
        out.println(row(headers, widths));
        out.println(separator(widths));
        for (String[] cell : cells)
            out.println(row(cell, widths));
        out.println("(" + rows + " row" + (rows == 1 ? "" : "s")
                    + (offset > 0 ? ", from offset " + offset : "")
                    + " — a row count is NOT a count; run the count form for that)");
    }

    private static String row(String[] cells, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int c = 0; c < cells.length; c++) {
            if (c > 0)
                line.append("  ");
            line.append(pad(cells[c], widths[c]));
        }
        return line.toString().stripTrailing();
    }

    private static String separator(int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            if (c > 0)
                line.append("  ");
            line.append("-".repeat(widths[c]));
        }
        return line.toString();
    }

    private static String pad(String value, int width) {
        return value.length() >= width ? value : value + " ".repeat(width - value.length());
    }
}
