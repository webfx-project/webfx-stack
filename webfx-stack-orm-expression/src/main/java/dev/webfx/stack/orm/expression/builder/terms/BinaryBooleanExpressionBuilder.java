package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.BinaryBooleanExpression;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.BinaryBooleanExpression;

/**
 * @author Bruno Salmon
 */
public abstract class BinaryBooleanExpressionBuilder extends BinaryExpressionBuilder {

    public BinaryBooleanExpressionBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        super(left, right);
    }

    @Override
    public BinaryBooleanExpression build() {
        return (BinaryBooleanExpression) super.build();
    }

    protected abstract BinaryBooleanExpression newBinaryOperation(Expression left, Expression right);
}
