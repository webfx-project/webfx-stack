package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;

/**
 * @author Bruno Salmon
 */
public final class NotLike<T> extends BinaryBooleanExpression<T> {

    public NotLike(Expression<T> left, Expression<T> right) {
        super(left, " not like ", right, 5);
    }

    @Override
    public Boolean evaluateCondition(Object a, Object b, DomainReader<T> domainReader) {
        return b instanceof String && !new Like.LikeImpl((String) b).compare(a);
    }

}
