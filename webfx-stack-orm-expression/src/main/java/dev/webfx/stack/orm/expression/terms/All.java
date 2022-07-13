package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;

/**
 * @author Bruno Salmon
 */
public final class All<T> extends BinaryBooleanExpression<T> {

    public All(Expression<T> left, String operator, Expression<T> right) {
        super(left, operator + " all ", right, 5);
    }

    public Boolean evaluateCondition(Object a, Object b, DomainReader<T> domainReader) {
        throw new UnsupportedOperationException();
    }
}
