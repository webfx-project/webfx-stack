package dev.webfx.stack.framework.shared.orm.expression.terms;

import dev.webfx.stack.framework.shared.orm.expression.Expression;

/**
 * @author Bruno Salmon
 */
public interface ParentExpression<T> extends Expression<T> {

    Expression<T>[] getChildren();

}
