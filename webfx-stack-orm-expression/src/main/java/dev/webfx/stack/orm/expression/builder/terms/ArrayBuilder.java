package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Array;

/**
 * @author Bruno Salmon
 */
public final class ArrayBuilder extends UnaryExpressionBuilder {

    public ArrayBuilder(ExpressionBuilder operand) {
        super(operand);
    }

    @Override
    protected Array newUnaryOperation(Expression operand) {
        return new Array(operand);
    }
}
