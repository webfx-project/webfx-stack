package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.terms.Exists;
import dev.webfx.stack.orm.expression.terms.SelectExpression;

/**
 * @author Bruno Salmon
 */
public final class SelectExpressionSqlCompiler extends AbstractTermSqlCompiler<SelectExpression> {

    public SelectExpressionSqlCompiler() {
        super(SelectExpression.class, Exists.class);
    }

    @Override
    public void compileExpressionToSql(SelectExpression e, Options o) {
        String leftBracket = "(";
        if (e instanceof Exists)
            leftBracket = "exists(";
        StringBuilder clauseBuilder = o.build.prepareAppend(o).append(leftBracket);
        compileSelect(e.getSelect(), o.changeReadForeignFields(false));
        clauseBuilder.append(')');
    }
}
