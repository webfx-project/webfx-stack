package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.Expression;

/**
 * @author Bruno Salmon
 */
public final class Cast<T> extends UnaryExpression<T> {

    private final String type;

    public Cast(Expression<T> operand, String type) {
        super(operand);
        this.type = type;
    }

    public String getCastType() {
        return type;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        return operand.toString(sb).append("::").append(type);
    }
}
