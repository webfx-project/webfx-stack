package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.NotEquals;

/**
 * @author Bruno Salmon
 */
public final class NotEqualsBuilder extends BinaryBooleanExpressionBuilder {

    public NotEqualsBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        super(left, right);
    }

    @Override
    protected NotEquals newBinaryOperation(Expression left, Expression right) {
        return new NotEquals(left, right);
    }
}
