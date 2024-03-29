package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.All;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.All;

/**
 * @author Bruno Salmon
 */
public final class AllBuilder extends BinaryBooleanExpressionBuilder {
    final String operator;

    public AllBuilder(ExpressionBuilder left, String operator, ExpressionBuilder right) {
        super(left, right);
        this.operator = operator;
    }

    @Override
    protected All newBinaryOperation(Expression left, Expression right) {
        return new All(left, operator, right);
    }
}
