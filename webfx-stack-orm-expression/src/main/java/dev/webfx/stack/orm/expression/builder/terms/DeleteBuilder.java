package dev.webfx.stack.orm.expression.builder.terms;


import dev.webfx.stack.orm.expression.terms.Delete;

/**
 * @author Bruno Salmon
 */
public final class DeleteBuilder extends DqlStatementBuilder<Delete> {
    public Object filterId;

    public DeleteBuilder() {
    }

    @Override
    protected Delete buildDqlOrder() {
        propagateDomainClasses();
        return new Delete(filterId, buildingClass, buildingClassAlias, definition, sqlDefinition, sqlParameters,
                where == null ? null : where.build());
    }
}
