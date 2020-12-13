package dev.webfx.framework.shared.orm.expression.terms;

import dev.webfx.framework.shared.orm.expression.Expression;

/**
 * @author Bruno Salmon
 */
public interface ParentExpression<T> extends Expression<T> {

    Expression<T>[] getChildren();

}
