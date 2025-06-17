package dev.webfx.stack.orm.expression.builder.terms;


import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.builder.ReferenceResolver;
import dev.webfx.stack.orm.expression.terms.Select;

/**
 * @author Bruno Salmon
 */
public final class SelectBuilder extends DqlOrderBuilder<Select> {
    public Object filterId;
    public boolean distinct = false;
    public boolean includeIdColumn = true;
    public ExpressionArrayBuilder fields;
    public ExpressionArrayBuilder groupBy;
    public ExpressionBuilder having;
    public ExpressionArrayBuilder orderBy;

    public SelectBuilder() {
    }

    @Override
    protected Select buildDqlOrder() {
        propagateDomainClasses();
        return new Select(filterId, buildingClass, buildingClassAlias, definition, sqlDefinition, sqlParameters,
                distinct,
                fields == null ? null : fields.build(),
                where == null ? null : where.build(),
                groupBy == null ? null : groupBy.build(),
                having == null ? null : having.build(),
                orderBy == null ? null : orderBy.build(),
                limit == null ? null : limit.build(),
                includeIdColumn);
    }

    @Override
    protected void propagateDomainClasses() {
        super.propagateDomainClasses();
        if (fields != null)
            fields.buildingClass = buildingClass;
        if (groupBy != null)
            groupBy.buildingClass = buildingClass;
        if (having != null)
            having.buildingClass = buildingClass;
        if (orderBy != null)
            orderBy.buildingClass = buildingClass;
    }

    @Override
    public Expression resolveReference(String name) {
        // Might be a reference to the building class
        Expression reference = super.resolveReference(name);
        // Or to a loaded field (or subquery) assigned to an alias
        if (reference == null && fields != null) {
            for (ExpressionBuilder fieldBuilder : fields.expressions) {
                if (fieldBuilder instanceof ReferenceResolver) // Ex: AsBuilder
                    reference = ((ReferenceResolver) fieldBuilder).resolveReference(name);
                if (reference != null)
                    break;
            }
        }
        return reference;
    }
}
