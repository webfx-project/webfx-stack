package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Any;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Any;

/**
 * @author Bruno Salmon
 */
public final class AnyBuilder extends BinaryBooleanExpressionBuilder {
    final String operator;

    public AnyBuilder(ExpressionBuilder left, String operator, ExpressionBuilder right) {
        super(left, right);
        this.operator = operator;
    }

    @Override
    protected Any newBinaryOperation(Expression left, Expression right) {
        return new Any(left, operator, right);
    }
}
