package dev.webfx.stack.orm.expression.builder.terms;


import dev.webfx.stack.orm.expression.terms.Update;
import dev.webfx.stack.orm.expression.terms.Update;

/**
 * @author Bruno Salmon
 */
public final class UpdateBuilder extends DqlOrderBuilder<Update> {
    public Object filterId;
    public ExpressionArrayBuilder setFields;

    public UpdateBuilder() {
    }

    @Override
    protected Update buildDqlOrder() {
        propagateDomainClasses();
        return new Update(filterId, buildingClass, buildingClassAlias, definition, sqlDefinition, sqlParameters,
                setFields.build(),
                where == null ? null : where.build()
                );
    }

    @Override
    protected void propagateDomainClasses() {
        super.propagateDomainClasses();
        if (setFields != null)
            setFields.buildingClass = buildingClass;
    }
}
