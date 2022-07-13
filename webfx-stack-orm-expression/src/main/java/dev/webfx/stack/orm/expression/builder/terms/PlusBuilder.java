package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Plus;

/**
 * @author Bruno Salmon
 */
public final class PlusBuilder extends BinaryExpressionBuilder {

    public PlusBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        super(left, right);
    }

    @Override
    protected Plus newBinaryOperation(Expression left, Expression right) {
        return new Plus(left, right);
    }
}
