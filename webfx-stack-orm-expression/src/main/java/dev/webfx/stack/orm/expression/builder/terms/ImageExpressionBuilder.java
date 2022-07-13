package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.UnaryExpression;
import dev.webfx.stack.orm.expression.terms.function.Call;

/**
 * @author Bruno Salmon
 */
public final class ImageExpressionBuilder extends UnaryExpressionBuilder {

    public ImageExpressionBuilder(ExpressionBuilder operand) {
        super(operand);
    }

    @Override
    protected UnaryExpression newUnaryOperation(Expression operand) {
        return new Call("image", operand); // temporary
    }
}
