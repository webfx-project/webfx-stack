package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;

/**
 * @author Bruno Salmon
 */
public final class ExpressionArraySqlCompiler extends AbstractTermSqlCompiler<ExpressionArray> {

    public ExpressionArraySqlCompiler() {
        super(ExpressionArray.class);
    }

    @Override
    public void compileExpressionToSql(ExpressionArray e, Options o) {
        for (Expression child : e.getExpressions())
            compileChildExpressionToSql(child, o);
    }
}
