package dev.webfx.stack.framework.shared.orm.expression.builder;

import dev.webfx.stack.framework.shared.orm.expression.Expression;

/**
 * @author Bruno Salmon
 */
public interface ReferenceResolver {

    Expression resolveReference(String name); // returns Alias or Field

}
