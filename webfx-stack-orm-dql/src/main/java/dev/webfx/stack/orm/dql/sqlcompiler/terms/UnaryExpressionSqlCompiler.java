package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.terms.Array;
import dev.webfx.stack.orm.expression.terms.As;
import dev.webfx.stack.orm.expression.terms.Not;
import dev.webfx.stack.orm.expression.terms.UnaryExpression;

/**
 * @author Bruno Salmon
 */
public final class UnaryExpressionSqlCompiler extends AbstractTermSqlCompiler<UnaryExpression<?>> {

    public UnaryExpressionSqlCompiler() {
        super(Array.class, As.class, Not.class);
    }

    @Override
    public void compileExpressionToSql(UnaryExpression<?> e, Options o) {
        if (e instanceof As<?> as) {
            compileChildExpressionToSql(e.getOperand(), o.changeGenerateQueryMapping(false));
            if (o.isTopLevelSelect()) {
                String alias = as.getAlias();
                o.build.addColumnInClause(null, alias, alias, null, o.clause, " as ", false, false, o.generateQueryMapping);
            }
        } else {
            String left, right;
            if (e instanceof Array) {
                left = "array[";
                right = "]";
            } else if (e instanceof Not) {
                left = "not (";
                right = ")";
            } else
                throw new IllegalArgumentException();
            StringBuilder clauseBuilder = o.build.prepareAppend(o).append(left);
            compileChildExpressionToSql(e.getOperand(), o);
            clauseBuilder.append(right);
        }
    }
}
