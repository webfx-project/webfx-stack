package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.terms.This;

/**
 * Compiles {@code this} - the row a data scope condition is filtering - to that row's primary key column.
 *
 * <p>It exists so that a scope condition can correlate a subquery back to the row it scopes. The rule for
 * Person, for instance, is that the caller may read a person who booked one of the events in their scope:
 * <pre>exists(select Document where event in $1 and person = this)</pre>
 * where {@code person = this} has to become {@code d.person_id = p.id} with {@code p} the alias of the
 * person being filtered.
 *
 * <p><b>Why not {@link IdSqlCompiler}, which already emits a primary key?</b> Because it takes its alias
 * from {@code build.getCompilingClass()}, the innermost build - inside the {@code exists} above that is
 * Document, so {@code id} would emit the subquery's own key and the correlation would be lost (the
 * predicate would read "some document of some person", true for nearly every person). {@code this} instead
 * reads the row recorded on the root build when the condition's compilation began, which is the row being
 * scoped however many subqueries deep the term appears.
 *
 * <p>Outside a scope condition {@code this} has no referent, and compiling it is refused rather than
 * guessed at: a client statement cannot reach this term, since the expression it comes from is
 * server-authored.
 *
 * @author Bruno Salmon
 */
public final class ThisSqlCompiler extends AbstractTermSqlCompiler<This> {

    public ThisSqlCompiler() {
        super(This.class);
    }

    @Override
    public void compileExpressionToSql(This e, Options o) {
        Object scopeRowClass = o.build.getScopeRowDomainClass();
        String scopeRowAlias = o.build.getScopeRowAlias();
        if (scopeRowClass == null || scopeRowAlias == null)
            throw new IllegalArgumentException("'this' means the row a data scope condition is filtering,"
                + " so it is only compilable inside such a condition - there is no row for it to stand for here");
        // fieldId null and generateQueryMapping false: a correlation term names no field of the result, and
        // addColumnInClause only builds a mapping for the SELECT clause anyway. isBoolean false: a primary key.
        o.build.addColumnInClause(scopeRowAlias, o.modelReader.getDomainClassPrimaryKeySqlColumnName(scopeRowClass),
            null, null, o.clause, o.separator, o.grouped, false, false);
    }
}
