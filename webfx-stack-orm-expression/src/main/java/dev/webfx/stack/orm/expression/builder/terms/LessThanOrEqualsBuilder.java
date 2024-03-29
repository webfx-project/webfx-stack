package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.LessThanOrEquals;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.LessThanOrEquals;

/**
 * @author Bruno Salmon
 */
public final class LessThanOrEqualsBuilder extends BinaryBooleanExpressionBuilder {

    public LessThanOrEqualsBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        super(left, right);
    }

    @Override
    protected LessThanOrEquals newBinaryOperation(Expression left, Expression right) {
        return new LessThanOrEquals(left, right);
    }
}
