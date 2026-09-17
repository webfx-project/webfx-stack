package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.function.Call;

/**
 * @author Bruno Salmon
 */
public final class CallBuilder extends UnaryExpressionBuilder {

    public final String functionName;
    public final ExpressionArrayBuilder orderBy;
    // Whether the argument should be preceded by the `distinct` keyword
    // (parsed from `funcName(distinct expr)`). Only meaningful for aggregate
    // functions — typically `count(distinct …)`.
    public final boolean distinct;

    public CallBuilder(String functionName) {
        this(functionName, null);
    }

    public CallBuilder(String functionName, ExpressionBuilder operand) {
        this(functionName, operand, null, false);
    }

    public CallBuilder(String functionName, ExpressionBuilder operand, ExpressionArrayBuilder orderBy) {
        this(functionName, operand, orderBy, false);
    }

    public CallBuilder(String functionName, ExpressionBuilder operand, ExpressionArrayBuilder orderBy, boolean distinct) {
        super(operand);
        this.functionName = functionName;
        this.orderBy = orderBy;
        this.distinct = distinct;
    }

    @Override
    protected void propagateDomainClasses() {
        super.propagateDomainClasses();
        if (orderBy != null)
            orderBy.buildingClass = buildingClass;
    }

    @Override
    protected Call newUnaryOperation(Expression operand) {
        return new Call(functionName, operand, orderBy == null ? null : orderBy.build(), distinct);
    }
}
