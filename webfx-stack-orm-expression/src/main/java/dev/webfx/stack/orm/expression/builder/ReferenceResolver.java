package dev.webfx.stack.orm.expression.builder;

import dev.webfx.stack.orm.expression.Expression;

/**
 * @author Bruno Salmon
 */
public interface ReferenceResolver {

    Expression resolveReference(String name); // returns Alias or Field

    // Optional: resolve a dot expression "aliasName.fieldName" where fieldName is a CTE field alias
    default Expression resolveDotReference(String aliasName, String fieldName) { return null; }

}
