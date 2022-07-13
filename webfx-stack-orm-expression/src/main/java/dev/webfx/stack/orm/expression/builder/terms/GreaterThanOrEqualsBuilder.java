package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.GreaterThanOrEquals;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.GreaterThanOrEquals;

/**
 * @author Bruno Salmon
 */
public final class GreaterThanOrEqualsBuilder extends BinaryBooleanExpressionBuilder {

    public GreaterThanOrEqualsBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        super(left, right);
    }

    @Override
    protected GreaterThanOrEquals newBinaryOperation(Expression left, Expression right) {
        return new GreaterThanOrEquals(left, right);
    }
}
