package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Cast;

/**
 * @author Bruno Salmon
 */
public final class CastBuilder extends UnaryExpressionBuilder {
    private final String type;

    public CastBuilder(ExpressionBuilder operand, String type) {
        super(operand);
        this.type = type;
    }

    @Override
    protected Cast newUnaryOperation(Expression operand) {
        return new Cast(operand, type);
    }
}
