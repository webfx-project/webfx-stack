package dev.webfx.stack.orm.dql.query.interceptor;

/**
 * Thrown from inside the compiler when a client's query reaches a denied table or column, to abandon the
 * compilation at the exact point it happened. Carries the names for the server log only: the client is told that
 * its query was refused, never which secret it reached for.
 *
 * @author Claude Code
 */
final class ClientReadDeniedException extends RuntimeException {

    final String table;
    final String column; // null when the whole table is denied

    ClientReadDeniedException(String table, String column) {
        super(column == null ? table : table + "." + column);
        this.table = table;
        this.column = column;
    }
}
