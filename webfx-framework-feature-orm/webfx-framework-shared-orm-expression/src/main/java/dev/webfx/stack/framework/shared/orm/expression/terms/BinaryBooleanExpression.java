package dev.webfx.stack.framework.shared.orm.expression.terms;

import dev.webfx.stack.framework.shared.orm.expression.Expression;
import dev.webfx.stack.framework.shared.orm.expression.lci.DomainReader;
import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.type.Type;

/**
 * @author Bruno Salmon
 */
public abstract class BinaryBooleanExpression<T> extends BinaryExpression<T> {

    protected BinaryBooleanExpression(Expression<T> left, String separator, Expression<T> right, int precedenceLevel) {
        super(left, separator, right, precedenceLevel);
    }

    @Override
    public Type getType() {
        return PrimType.BOOLEAN;
    }

    @Override
    public Object evaluate(Object leftValue, Object rightValue, DomainReader<T> domainReader) {
        return evaluateCondition(leftValue, rightValue, domainReader);
    }

    public abstract Boolean evaluateCondition(Object a, Object b, DomainReader<T> domainReader);

}
