package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlClause;
import dev.webfx.stack.orm.expression.terms.Dot;
import dev.webfx.stack.orm.expression.terms.HasDomainClass;
import dev.webfx.stack.orm.expression.terms.Symbol;
import dev.webfx.extras.type.Types;

/**
 * @author Bruno Salmon
 */
public final class SymbolSqlCompiler extends AbstractTermSqlCompiler<Symbol> {

    public SymbolSqlCompiler() {
        super(Symbol.class);
    }

    @Override
    public void compileExpressionToSql(Symbol e, Options o) {
        Expression expression = e.getExpression();
        if (expression != null) { // Ex: a domain field which is actually an expression.
            // Should the expression be directly compiled in SQL (1), or should we load the persistent fields for its
            // later evaluation on the client-side (2)?
            // We want (2) for example, when we load bookingFormUrl in FXFestivals, so that when BookingStarter evaluates
            // event.onExpressionLoaded("kbs3,bookingFormUrl") it doesn't need to load bookingFormUrl again
            if (o.generateQueryMapping) // We are in select with query mapping
                compileExpressionPersistentTermsToSql(expression, o); // (2) => we load persistent fields
            else // Ex: in a where or order by clause, or inside an As expression to be stored in an alias
                compileChildExpressionToSql(expression, o); // (1) => we need to evaluate the expression directly in SQL
        } else {
            Expression foreignField = null;
            Object termDomainClass = e instanceof HasDomainClass /*ex: domain field */ ? ((HasDomainClass) e).getDomainClass() : o.build.getCompilingClass();
            if (o.clause == SqlClause.SELECT && o.readForeignFields) {
                Object foreignClass = o.modelReader.getSymbolForeignDomainClass(termDomainClass, e, false);
                if (foreignClass != null /* && build.getJoinMapping() == null  to avoid infinite recursion, see item.icon*/)
                    foreignField = o.modelReader.getDomainClassDefaultForeignFields(foreignClass);
            }
            if (foreignField == null)
                o.build.addColumnInClause(o.build.getClassAlias(termDomainClass, o.modelReader), o.modelReader.getSymbolSqlColumnName(termDomainClass, e), e, o.modelReader.getSymbolForeignDomainClass(termDomainClass, e, false), o.clause, o.separator, o.grouped, Types.isBooleanType(e.getType()), o.generateQueryMapping);
            else
                compileChildExpressionToSql(Dot.dot(e, foreignField, true), o);
        }
    }
}
