package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Minus;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Minus;

/**
 * @author Bruno Salmon
 */
public final class MinusBuilder extends BinaryExpressionBuilder {

    public MinusBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        super(left, right);
    }

    @Override
    protected Minus newBinaryOperation(Expression left, Expression right) {
        return new Minus(left, right);
    }
}
