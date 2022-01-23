package dev.webfx.framework.shared.orm.expression.builder.terms;


import dev.webfx.framework.shared.orm.expression.terms.Insert;

/**
 * @author Bruno Salmon
 */
public final class InsertBuilder extends DqlOrderBuilder<Insert> {
    public Object filterId;
    public ExpressionArrayBuilder setFields;

    public InsertBuilder() {
    }

    @Override
    protected Insert buildDqlOrder() {
        propagateDomainClasses();
        return new Insert(filterId, buildingClass, buildingClassAlias, definition, sqlDefinition, sqlParameters,
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
