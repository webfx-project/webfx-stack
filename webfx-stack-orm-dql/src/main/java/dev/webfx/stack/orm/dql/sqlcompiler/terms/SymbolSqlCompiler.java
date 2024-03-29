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
        if (e.getExpression() != null)
            compileChildExpressionToSql(e.getExpression(), o);
        else {
            Expression foreignField = null;
            Object termDomainClass = e instanceof HasDomainClass /*ex: domain field */ ? ((HasDomainClass) e).getDomainClass() : o.build.getCompilingClass();
            if (o.readForeignFields && o.clause == SqlClause.SELECT) {
                Object foreignClass = o.modelReader.getSymbolForeignDomainClass(termDomainClass, e, false);
                if (foreignClass != null /* && build.getJoinMapping() == null  to avoid infinite recursion, see item.icon*/)
                    foreignField = o.modelReader.getDomainClassDefaultForeignFields(foreignClass);
            }
            if (foreignField == null)
                o.build.addColumnInClause(o.build.getClassAlias(termDomainClass, o.modelReader), o.modelReader.getSymbolSqlColumnName(termDomainClass, e), e, o.modelReader.getSymbolForeignDomainClass(termDomainClass, e, false), o.clause, o.separator, o.grouped, Types.isBooleanType(e.getType()), o.generateQueryMapping);
            else
                compileChildExpressionToSql(new Dot(e, foreignField, true), o);
        }
    }
}
