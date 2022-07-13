package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.NotLike;

/**
 * @author Bruno Salmon
 */
public final class NotLikeBuilder extends BinaryBooleanExpressionBuilder {

    public NotLikeBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        super(left, right);
    }

    @Override
    protected NotLike newBinaryOperation(Expression left, Expression right) {
        return new NotLike(left, right);
    }
}
