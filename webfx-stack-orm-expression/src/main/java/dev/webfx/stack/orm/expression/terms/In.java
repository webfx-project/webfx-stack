package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.platform.util.Arrays;

import java.util.Collection;

/**
 * @author Bruno Salmon
 */
public final class In<T> extends BinaryBooleanExpression<T> {

    public In(Expression<T> left, Expression<T> right) {
        super(left, " in ", right, 5);
    }

    @Override
    public Boolean evaluateCondition(Object a, Object b, DomainReader<T> domainReader) {
        if (b instanceof Object[])
            return Arrays.contains((Object[]) b, a);
        if (b instanceof Collection)
            return ((Collection) b).contains(a);
        return false;
    }
}
