package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;

/**
 * @author Bruno Salmon
 */
public final class Any<T> extends BinaryBooleanExpression<T> {

    public Any(Expression<T> left, String operator, Expression<T> right) {
        super(left, operator + " any ", right, 5);
    }

    public Boolean evaluateCondition(Object a, Object b, DomainReader<T> domainReader) {
        throw new UnsupportedOperationException();
    }
}
