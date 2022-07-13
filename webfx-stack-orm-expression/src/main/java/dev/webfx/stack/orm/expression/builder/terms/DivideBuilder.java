package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Divide;

/**
 * @author Bruno Salmon
 */
public final class DivideBuilder extends BinaryExpressionBuilder {

    public DivideBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        super(left, right);
    }

    @Override
    protected Divide newBinaryOperation(Expression left, Expression right) {
        return new Divide(left, right);
    }
}
