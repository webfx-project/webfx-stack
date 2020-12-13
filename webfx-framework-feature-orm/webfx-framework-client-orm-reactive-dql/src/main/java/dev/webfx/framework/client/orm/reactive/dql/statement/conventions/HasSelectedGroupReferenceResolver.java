package dev.webfx.framework.client.orm.reactive.dql.statement.conventions;

import dev.webfx.framework.shared.orm.expression.builder.ReferenceResolver;

public interface HasSelectedGroupReferenceResolver {

    ReferenceResolver getSelectedGroupReferenceResolver();

    void setSelectedGroupReferenceResolver(ReferenceResolver referenceResolver);
}
