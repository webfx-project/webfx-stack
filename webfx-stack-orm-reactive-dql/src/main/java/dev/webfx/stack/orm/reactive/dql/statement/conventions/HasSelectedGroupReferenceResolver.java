package dev.webfx.stack.orm.reactive.dql.statement.conventions;

import dev.webfx.stack.orm.expression.builder.ReferenceResolver;

public interface HasSelectedGroupReferenceResolver {

    ReferenceResolver getSelectedGroupReferenceResolver();

    void setSelectedGroupReferenceResolver(ReferenceResolver referenceResolver);
}
