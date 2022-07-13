package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.Expression;

/**
 * @author Bruno Salmon
 */
public interface ParentExpression<T> extends Expression<T> {

    Expression<T>[] getChildren();

}
