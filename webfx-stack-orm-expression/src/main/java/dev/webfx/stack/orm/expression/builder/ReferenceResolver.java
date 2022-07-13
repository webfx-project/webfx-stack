package dev.webfx.stack.orm.expression.builder;

import dev.webfx.stack.orm.expression.Expression;

/**
 * @author Bruno Salmon
 */
public interface ReferenceResolver {

    Expression resolveReference(String name); // returns Alias or Field

}
