package dev.webfx.stack.orm.dql.query.interceptor;

import dev.webfx.stack.db.query.ClientReadDenyList;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.dql.sqlcompiler.lci.CompilerDomainModelReader;
import dev.webfx.stack.orm.expression.Expression;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The compiler's view of the domain model, with the client-secret tables and columns removed from it.
 *
 * <p>This is where the check can be exhaustive rather than hopeful. The compiler states, in its own interface,
 * that every time it needs the domain model it asks this reader — and every table name in the SQL it emits
 * (FROM, every join, common-table entities, parameter references) and every column (selected, filtered, sorted,
 * joined on, or reached through a dot path from some unrelated root) is obtained from one of three methods here.
 * So a query that touches a denied column cannot be compiled through this reader at all, however it is phrased,
 * and nothing had to predict the phrasing. A traversal of the parsed statement, by contrast, would be only as
 * complete as its author's memory of every construct the grammar allows.
 *
 * <p>Only the methods that put a NAME into the SQL are checked. The other two tell the compiler how to navigate
 * — which class a foreign key points at, which fields to load by default — and emit nothing on their own; any
 * column they lead to is still named through a checked method.
 *
 * @author Claude Code
 */
final class DenyingCompilerDomainModelReader implements CompilerDomainModelReader {

    private final CompilerDomainModelReader delegate;
    /**
     * How many times the statement reached a column that may be TESTED but not read — see
     * {@link ClientReadDenyList#denyColumnExceptEqualityMatch}. Recorded rather than refused, because whether
     * the reaching was legitimate is a question about WHERE in the statement it happened, and this reader is
     * context-free by design. {@link CapabilityColumnWalk} answers it; this only raises it.
     *
     * <p><b>A count and not a flag, and that distinction is the whole control.</b> The walk reads the statement
     * and this reads what the compiler emits, and the two come apart — a fields group, a field defined as an
     * expression and a bare foreign key's default fields all put a column into the SQL without it appearing in
     * the statement. A flag could only ever say "at least one", so a statement carrying ONE legitimate test
     * licensed every hidden read beside it. Counting lets the two be required to AGREE, which closes the class
     * rather than each construct in it.
     */
    private int capabilityColumnHits;
    /**
     * WATCH MODE: deny nothing, and count the WATCHED capability columns instead of the enforced ones.
     *
     * <p>A mode rather than a second class, and the modes are kept apart rather than blended: the counting
     * rule below is a control, and the one bug it has already had was a count that conflated two things. An
     * observation that shared the enforced set's counter would put the same defect back — a watched column's
     * hit would be weighed against the walk's sanctioned count for the ENFORCED ones, and the two are not
     * measuring the same statement positions.
     *
     * <p>Denying nothing matters too. This reader runs on the observation path, where refusing is not
     * available: if it threw on an enforced secret the observation would simply stop, and it would stop
     * precisely on the statements most worth describing.
     */
    private final boolean watchOnly;
    private final Set<String> watchedColumnsReached = new LinkedHashSet<>();
    /**
     * Flat-watched columns this statement reached — see {@code ClientReadDenyList.observeColumn}.
     *
     * <p>Kept APART from {@link #watchedColumnsReached} and counted in no tally of its own, because the walk
     * that reconciles capability hits against equality tests must never see these: a flat column has no
     * legitimate equality use, so an equality test elsewhere in the statement could otherwise excuse a read of
     * it. Every entry here is a read, full stop, and the inspector reports them without consulting the walk.
     */
    private final Set<String> flatWatchedColumnsReached = new LinkedHashSet<>();

    DenyingCompilerDomainModelReader(CompilerDomainModelReader delegate) {
        this(delegate, false);
    }

    DenyingCompilerDomainModelReader(CompilerDomainModelReader delegate, boolean watchOnly) {
        this.delegate = delegate;
        this.watchOnly = watchOnly;
    }

    /** Flat-watched columns reached, each once. Every one is a read; there is nothing to reconcile. */
    Set<String> flatWatchedColumnsReached() {
        return flatWatchedColumnsReached;
    }

    int capabilityColumnHits() {
        return capabilityColumnHits;
    }

    @Override
    public String getDomainClassSqlTableName(Object domainClass) {
        String table = delegate.getDomainClassSqlTableName(domainClass);
        if (!watchOnly && ClientReadDenyList.isTableDenied(table))
            throw new ClientReadDeniedException(table, null);
        return table;
    }

    @Override
    public String getDomainClassPrimaryKeySqlColumnName(Object domainClass) {
        String column = delegate.getDomainClassPrimaryKeySqlColumnName(domainClass);
        String table = delegate.getDomainClassSqlTableName(domainClass);
        if (watchOnly) {
            // Counted here too, though no token in this system is a primary key. The enforced rule DENIES a
            // capability primary key outright through isColumnDenied above, so a watch that skipped this method
            // would report a clean count for a column it never looked at — and clean is the answer that
            // restores the enforced rule.
            if (ClientReadDenyList.isObservedCapabilityColumn(table, column)) {
                capabilityColumnHits++;
                watchedColumnsReached.add(table + "." + column);
            } else if (ClientReadDenyList.isObservedColumn(table, column)) {
                flatWatchedColumnsReached.add(table + "." + column);
            }
            return column;
        }
        if (ClientReadDenyList.isColumnDenied(table, column))
            throw new ClientReadDeniedException(table, column);
        return column;
    }

    @Override
    public String getSymbolSqlColumnName(Object symbolDomainClass, Expression symbol) {
        String column = delegate.getSymbolSqlColumnName(symbolDomainClass, symbol);
        String table = tableOf(symbolDomainClass, symbol);
        if (watchOnly) {
            // Watching: count the watched set, deny nothing — not even a table or column denied outright,
            // which the enforcing pass has already dealt with on its own compilation.
            if (ClientReadDenyList.isObservedCapabilityColumn(table, column)) {
                capabilityColumnHits++;
                watchedColumnsReached.add(table + "." + column);
            } else if (ClientReadDenyList.isObservedColumn(table, column)) {
                flatWatchedColumnsReached.add(table + "." + column);
            }
            return column;
        }
        // Asked FIRST, because isColumnDenied answers true for these too — deliberately, so that a consumer
        // which has not learned about the third category refuses one rather than handing it over.
        if (ClientReadDenyList.isCapabilityColumn(table, column)) {
            capabilityColumnHits++;
            return column;
        }
        if (ClientReadDenyList.isColumnDenied(table, column))
            throw new ClientReadDeniedException(table, column);
        return column;
    }

    /**
     * Which watched columns the SQL reached, for the report. Names only — a table and a column out of the
     * domain model, never a value. Distinct, unlike {@link #capabilityColumnHits}, which has to stay a count
     * because the walk's rule is that the two counts AGREE.
     */
    Set<String> watchedColumnsReached() {
        return watchedColumnsReached;
    }

    @Override
    public Object getSymbolForeignDomainClass(Object symbolDomainClass, Expression symbol, boolean required) {
        return delegate.getSymbolForeignDomainClass(symbolDomainClass, symbol, required);
    }

    @Override
    public Expression getDomainClassDefaultForeignFields(Object domainClass) {
        return delegate.getDomainClassDefaultForeignFields(domainClass);
    }

    /**
     * The table a column belongs to. Taken from the field itself where there is one — a persistent field knows its
     * own class, which is more certain than the class the compiler happens to pass beside it — and from that class
     * otherwise (a primary key, which is not a field).
     */
    private String tableOf(Object symbolDomainClass, Expression symbol) {
        if (symbol instanceof DomainField field) {
            DomainClass fieldClass = field.getDomainClass();
            if (fieldClass != null)
                return fieldClass.getSqlTableName();
        }
        return symbolDomainClass == null ? null : delegate.getDomainClassSqlTableName(symbolDomainClass);
    }
}
