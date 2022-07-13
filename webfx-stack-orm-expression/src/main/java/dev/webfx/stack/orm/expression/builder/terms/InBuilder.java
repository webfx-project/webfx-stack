package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.In;

/**
 * @author Bruno Salmon
 */
public final class InBuilder extends BinaryBooleanExpressionBuilder {

    public InBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        super(left, right);
    }

    @Override
    protected In newBinaryOperation(Expression left, Expression right) {
        return new In(left, right);
    }
}
