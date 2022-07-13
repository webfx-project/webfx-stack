package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Like;

/**
 * @author Bruno Salmon
 */
public final class LikeBuilder extends BinaryBooleanExpressionBuilder {

    public LikeBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        super(left, right);
    }

    @Override
    protected Like newBinaryOperation(Expression left, Expression right) {
        return new Like(left, right);
    }
}
