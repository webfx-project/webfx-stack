package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.This;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.This;

/**
 * @author Bruno Salmon
 */
public final class ThisBuilder extends ExpressionBuilder {

    public static final ThisBuilder SINGLETON = new ThisBuilder();

    private ThisBuilder() {
    }

    @Override
    public Expression build() {
        return This.SINGLETON;
    }
}
