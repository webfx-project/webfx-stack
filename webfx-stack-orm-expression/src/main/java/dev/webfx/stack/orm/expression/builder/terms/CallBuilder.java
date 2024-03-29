package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.function.Call;

/**
 * @author Bruno Salmon
 */
public final class CallBuilder extends UnaryExpressionBuilder {

    public final String functionName;
    public final ExpressionArrayBuilder orderBy;

    public CallBuilder(String functionName) {
        this(functionName, null);
    }

    public CallBuilder(String functionName, ExpressionBuilder operand) {
        this(functionName, operand, null);
    }

    public CallBuilder(String functionName, ExpressionBuilder operand, ExpressionArrayBuilder orderBy) {
        super(operand);
        this.functionName = functionName;
        this.orderBy = orderBy;
    }

    @Override
    protected void propagateDomainClasses() {
        super.propagateDomainClasses();
        if (orderBy != null)
            orderBy.buildingClass = buildingClass;
    }

    @Override
    protected Call newUnaryOperation(Expression operand) {
        return new Call(functionName, operand, orderBy == null ? null : orderBy.build());
    }
}
