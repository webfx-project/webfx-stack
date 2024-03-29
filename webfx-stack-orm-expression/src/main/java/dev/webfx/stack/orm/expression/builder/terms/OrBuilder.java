package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Or;

/**
 * @author Bruno Salmon
 */
public final class OrBuilder extends BinaryExpressionBuilder {

    public OrBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        super(left, right);
    }

    @Override
    protected Or newBinaryOperation(Expression left, Expression right) {
        return new Or(left, right);
    }
}
