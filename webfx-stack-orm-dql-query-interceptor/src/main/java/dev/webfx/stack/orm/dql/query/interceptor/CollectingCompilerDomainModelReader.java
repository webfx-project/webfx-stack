package dev.webfx.stack.orm.dql.query.interceptor;

import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.dql.sqlcompiler.lci.CompilerDomainModelReader;
import dev.webfx.stack.orm.expression.Expression;

import java.util.Set;

/**
 * The compiler's view of the domain model, with a note taken of every table it names.
 *
 * <p>The observing twin of {@link DenyingCompilerDomainModelReader}, and deliberately a separate class rather
 * than a flag on it. That one decides and can throw; this one only watches and never does. Sharing them would
 * have meant a statement's description depending on where the denial happened to stop — half a list of tables,
 * for the statements it is most interesting to describe.
 *
 * <p>Why the reader rather than a traversal of the parsed statement, which is the obvious alternative: the
 * compiler states in its own interface that every time it needs the domain model it asks this reader, so every
 * table in the SQL it emits — the FROM, every join, a common-table entity, a table reached by a dot path from
 * some unrelated root, and the table a bare foreign key pulls in by loading its target's default fields — passes
 * through here. A traversal would be only as complete as its author's memory of the grammar, and the bare
 * foreign key is exactly the case such an author forgets.
 *
 * @author Claude Code
 */
final class CollectingCompilerDomainModelReader implements CompilerDomainModelReader {

    private final CompilerDomainModelReader delegate;
    private final Set<String> tables;

    CollectingCompilerDomainModelReader(CompilerDomainModelReader delegate, Set<String> tables) {
        this.delegate = delegate;
        this.tables = tables;
    }

    @Override
    public String getDomainClassSqlTableName(Object domainClass) {
        String table = delegate.getDomainClassSqlTableName(domainClass);
        if (table != null)
            tables.add(table);
        return table;
    }

    @Override
    public String getDomainClassPrimaryKeySqlColumnName(Object domainClass) {
        // Noted for its TABLE only. A primary key is not a disclosure worth naming per column, and recording the
        // column here would put "id" in every description ever produced.
        getDomainClassSqlTableName(domainClass);
        return delegate.getDomainClassPrimaryKeySqlColumnName(domainClass);
    }

    @Override
    public String getSymbolSqlColumnName(Object symbolDomainClass, Expression symbol) {
        String table = tableOf(symbolDomainClass, symbol);
        if (table != null)
            tables.add(table);
        return delegate.getSymbolSqlColumnName(symbolDomainClass, symbol);
    }

    @Override
    public Object getSymbolForeignDomainClass(Object symbolDomainClass, Expression symbol, boolean required) {
        return delegate.getSymbolForeignDomainClass(symbolDomainClass, symbol, required);
    }

    @Override
    public Expression getDomainClassDefaultForeignFields(Object domainClass) {
        return delegate.getDomainClassDefaultForeignFields(domainClass);
    }

    /** The table a column belongs to — from the field itself where there is one, as the denying reader does. */
    private String tableOf(Object symbolDomainClass, Expression symbol) {
        if (symbol instanceof DomainField field) {
            DomainClass fieldClass = field.getDomainClass();
            if (fieldClass != null)
                return fieldClass.getSqlTableName();
        }
        return symbolDomainClass == null ? null : delegate.getDomainClassSqlTableName(symbolDomainClass);
    }
}
