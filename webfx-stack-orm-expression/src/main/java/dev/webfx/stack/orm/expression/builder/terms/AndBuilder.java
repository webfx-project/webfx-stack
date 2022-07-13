package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.And;

/**
 * @author Bruno Salmon
 */
public final class AndBuilder extends BinaryExpressionBuilder {

    public AndBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        super(left, right);
    }

    @Override
    protected And newBinaryOperation(Expression left, Expression right) {
        return new And(left, right);
    }
}
