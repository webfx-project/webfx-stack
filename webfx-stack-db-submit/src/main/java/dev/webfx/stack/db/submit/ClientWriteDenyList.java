package dev.webfx.stack.db.submit;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Columns a CLIENT's write may never set, whatever else it changes on the row.
 *
 * <h3>What belongs here</h3>
 *
 * <p>Columns whose value decides who can sign in as whom: a password hash, the sign-in username, the flag that
 * disables an account — and whole tables that are nothing but credentials, such as sign-in links, which a client
 * must not be able to mint. A client that could set one of these could sign in as somebody else, or undo a control
 * meant to stop them, so no client writes them at all — the server does, through the flows that prove who is
 * asking (a password change, an email change, an administrator's decision). It is not an authorisation model:
 * whose row a client may write is a different question, answered elsewhere.
 *
 * <h3>Why names at the SQL level</h3>
 *
 * <p>Matched against the table and column names the statement resolves to, as {@link
 * dev.webfx.stack.db.submit.ClientSubmitGuard}'s inspector reads them from the domain model, not against the words
 * in the statement — so an alias or a differently spelled field name cannot rephrase a write around the list.
 *
 * <p>Plain collections and copy-on-write, because this class is compiled into the browser as well and only the
 * JRE that GWT emulates is available. Entries are added at boot and read on every client write.
 *
 * @author Claude Code
 */
public final class ClientWriteDenyList {

    /** Tables no client may write at all — insert, update or delete. Lower-cased. */
    private static volatile Set<String> deniedTables = Collections.emptySet();
    /** "table.column", lower-cased. */
    private static volatile Set<String> deniedColumns = Collections.emptySet();
    /** Tables that have at least one denied column — lower-cased. */
    private static volatile Set<String> guardedTables = Collections.emptySet();

    private ClientWriteDenyList() {}

    /** Denies every write to this table — for tables that are nothing but credentials, such as sign-in links. */
    public static synchronized void denyTable(String sqlTableName) {
        Set<String> next = new HashSet<>(deniedTables);
        if (next.add(normalise(sqlTableName)))
            deniedTables = Collections.unmodifiableSet(next);
    }

    public static boolean isTableDenied(String sqlTableName) {
        return sqlTableName != null && deniedTables.contains(normalise(sqlTableName));
    }

    /** Denies one column to client writes, leaving the rest of its table writable. */
    public static synchronized void denyColumn(String sqlTableName, String sqlColumnName) {
        String table = normalise(sqlTableName);
        Set<String> nextColumns = new HashSet<>(deniedColumns);
        if (nextColumns.add(table + "." + normalise(sqlColumnName))) {
            deniedColumns = Collections.unmodifiableSet(nextColumns);
            Set<String> nextTables = new HashSet<>(guardedTables);
            nextTables.add(table);
            guardedTables = Collections.unmodifiableSet(nextTables);
        }
    }

    public static boolean isColumnDenied(String sqlTableName, String sqlColumnName) {
        return sqlTableName != null && sqlColumnName != null
               && (isTableDenied(sqlTableName) || deniedColumns.contains(normalise(sqlTableName) + "." + normalise(sqlColumnName)));
    }

    /**
     * Whether this table has any denied column — a write to it that cannot be read field by field must then be
     * refused, since it might be setting one.
     */
    public static boolean isTableGuarded(String sqlTableName) {
        return sqlTableName != null && (isTableDenied(sqlTableName) || guardedTables.contains(normalise(sqlTableName)));
    }

    /** True when nothing has been declared, so there is nothing for an inspector to enforce. */
    public static boolean isEmpty() {
        return deniedTables.isEmpty() && deniedColumns.isEmpty();
    }

    /** Lower-cased and stripped of quoting and schema — a mismatch here would fail OPEN. */
    private static String normalise(String name) {
        String n = name.trim().toLowerCase();
        int dot = n.lastIndexOf('.');
        if (dot >= 0)
            n = n.substring(dot + 1);
        return n.replace("\"", "");
    }
}
