package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlClause;
import dev.webfx.stack.orm.expression.terms.Dot;
import dev.webfx.stack.orm.expression.terms.HasDomainClass;
import dev.webfx.stack.orm.expression.terms.ParameterReference;
import dev.webfx.stack.orm.expression.terms.Symbol;
import dev.webfx.extras.type.Types;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class SymbolSqlCompiler extends AbstractTermSqlCompiler<Symbol<?>> {

    public SymbolSqlCompiler() {
        super(Symbol.class);
    }

    @Override
    public void compileExpressionToSql(Symbol<?> e, Options o) {
        Expression<?> expression = e.getExpression();
        if (expression != null) { // Ex: a domain field which is actually an expression.
            // Should the expression be directly compiled in SQL (1), or should we load the persistent fields for its
            // later evaluation on the client-side (2), or should it be compiled to SQL with an alias mapping (3)?
            // We want (2) for example, when we load bookingFormUrl in FXFestivals, so that when BookingStarter evaluates
            // event.onExpressionLoaded("kbs3,bookingFormUrl") it doesn't need to load bookingFormUrl again
            // We want (3) when the client has no DQL runtime (e.g. React) and can't evaluate expressions locally
            if (o.generateQueryMapping) { // We are in select with query mapping
                if (o.compileExpressions && isComputedExpression(expression) && isSelfContainedExpression(e, expression, o)) {
                    // (3) => compile expression to SQL and map result column to the field name
                    // (same pattern as UnaryExpressionSqlCompiler for As expressions)
                    compileChildExpressionToSql(expression, o.changeGenerateQueryMapping(false));
                    // Use table-qualified alias if the simple name collides (e.g., both fullName
                    // and accountPerson.fullName in the same select)
                    String aliasName = e.getName();
                    if (o.build.hasColumnMapping(aliasName))
                        aliasName = o.build.getCompilingTableAlias() + "_" + aliasName;
                    o.build.addColumnInClause(null, aliasName, e, null, o.clause, " as ", false, false, o.generateQueryMapping);
                } else {
                    compileExpressionPersistentTermsToSql(expression, o); // (2) => we load persistent fields
                }
            } else // Ex: in a where or order by clause, or inside an As expression to be stored in an alias
                compileChildExpressionToSql(expression, o); // (1) => we need to evaluate the expression directly in SQL
        } else {
            Expression<?> foreignField = null;
            Object termDomainClass = e instanceof HasDomainClass hasDomainClass /*ex: domain field */ ? hasDomainClass.getDomainClass() : o.build.getCompilingClass();
            if (o.clause == SqlClause.SELECT && o.readForeignFields) {
                Object foreignClass = o.modelReader.getSymbolForeignDomainClass(termDomainClass, e, false);
                if (foreignClass != null /* && build.getJoinMapping() == null  to avoid infinite recursion, see item.icon*/)
                    foreignField = o.modelReader.getDomainClassDefaultForeignFields(foreignClass);
            }
            if (foreignField == null)
                o.build.addColumnInClause(o.build.getClassAlias(termDomainClass, o.modelReader), o.modelReader.getSymbolSqlColumnName(termDomainClass, e), e, o.modelReader.getSymbolForeignDomainClass(termDomainClass, e, false), o.clause, o.separator, o.grouped, Types.isBooleanType(e.getType()), o.generateQueryMapping);
            else
                // Implicit foreignFields must use compileExpressions=false so that expression
                // fields (e.g. Event's "icon") load their terminal persistent fields instead of
                // being compiled to SQL with a single alias — otherwise the mapping column count
                // doesn't match the SQL column count, causing index shifts.
                compileChildExpressionToSql(Dot.dot(e, foreignField, true), o.changeCompileExpressions(false));
        }
    }

    /**
     * Check if an expression is a computed expression that combines multiple persistent fields (e.g.,
     * fullName = firstName + ' ' + lastName). Simple field aliases (e.g., id) have only one persistent
     * term and don't need SQL compilation — they can use the standard persistent terms path.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean isComputedExpression(Expression<?> expression) {
        List<Expression> persistentTerms = new ArrayList<>();
        ((Expression) expression).collectPersistentTerms(persistentTerms);
        return persistentTerms.size() > 1;
    }

    /**
     * Check if an expression only references persistent fields from the same domain class as the symbol,
     * and none of them are foreign keys. Expressions that reference foreign fields (e.g. type.icon)
     * or client-only parameters (e.g. ?lang) can't be safely compiled to SQL inline — they should fall
     * back to loading all persistent fields so the client can evaluate the expression locally.
     */
    private static boolean isSelfContainedExpression(Symbol<?> symbol, Expression<?> expression, Options o) {
        if (!(symbol instanceof HasDomainClass hasDomainClass))
            return true;
        Object domainClass = hasDomainClass.getDomainClass();
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<Expression> persistentTerms = new ArrayList<>();
        ((Expression) expression).collectPersistentTerms(persistentTerms);
        for (Expression term : persistentTerms) {
            // Client-only parameters (e.g. ?lang) can't be inlined in SQL — the expression must be
            // evaluated locally by the client after all persistent fields have been loaded.
            if (term instanceof ParameterReference<?> param && param.isClientOnlyParameter(false))
                return false;
            if (term instanceof HasDomainClass termHasDc) {
                Object termDomainClass = termHasDc.getDomainClass();
                if (!domainClass.equals(termDomainClass))
                    return false;
                // Also reject if the term is a foreign key (e.g., type in type.icon) —
                // even though the FK field itself is from this domain class, the expression
                // traverses a relationship and involves joins
                if (o.modelReader.getSymbolForeignDomainClass(termDomainClass, term, false) != null)
                    return false;
            }
        }
        return true;
    }
}
