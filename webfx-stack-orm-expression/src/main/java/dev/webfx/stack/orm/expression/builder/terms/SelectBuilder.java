package dev.webfx.stack.orm.expression.builder.terms;


import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.builder.ReferenceResolver;
import dev.webfx.stack.orm.expression.terms.*;
import dev.webfx.stack.orm.expression.terms.function.Call;
import dev.webfx.stack.orm.expression.terms.function.Function;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class SelectBuilder extends DqlStatementBuilder<Select> {
    public Object filterId;
    public boolean distinct = false;
    public boolean includeIdColumn = true;
    public boolean useRowNumberAsId;
    public List<String[]> additionalFromEntities; // each entry: [className, alias]
    public ExpressionArrayBuilder fields;
    public ExpressionArrayBuilder groupBy;
    public ExpressionBuilder having;
    public ExpressionArrayBuilder orderBy;
    public ExpressionBuilder offset;

    public SelectBuilder() {
    }

    public void addAdditionalFromEntity(String className, String alias) {
        if (additionalFromEntities == null)
            additionalFromEntities = new ArrayList<>();
        additionalFromEntities.add(new String[]{className, alias});
    }

    @Override
    protected Select buildDqlOrder() {
        propagateDomainClasses();
        ExpressionArray fieldsArray = fields == null ? null : fields.build();
        if (groupBy == null && containsAggregateFunction(fieldsArray)) {
            useRowNumberAsId = true;
        }
        // Resolve additional FROM entity class names to domain class objects
        List<Object[]> resolvedAdditionalEntities = null;
        if (additionalFromEntities != null) {
            resolvedAdditionalEntities = new ArrayList<>(additionalFromEntities.size());
            for (String[] entry : additionalFromEntities)
                resolvedAdditionalEntities.add(new Object[]{getModelReader().getDomainClassByName(entry[0]), entry[1]});
        }
        return new Select(filterId, buildingClass, buildingClassAlias, definition, sqlDefinition, sqlParameters,
            distinct,
            fieldsArray,
            where == null ? null : where.build(),
            groupBy == null ? null : groupBy.build(),
            having == null ? null : having.build(),
            orderBy == null ? null : orderBy.build(),
            limit == null ? null : limit.build(),
            offset == null ? null : offset.build(),
            includeIdColumn,
            useRowNumberAsId,
            resolvedAdditionalEntities
            );
    }

    private static boolean containsAggregateFunction(ExpressionArray fieldsArray) {
        if (fieldsArray != null) {
            for (Expression e : fieldsArray.getExpressions()) {
                if (containsAggregateFunction(e))
                    return true;
            }
        }
        return false;
    }

    private static boolean containsAggregateFunction(Expression e) {
        if (e instanceof As<?> as)
            e = as.getOperand();
        if (e instanceof Call<?> call) {
            Function<?> function = call.getFunction();
            return function != null && function.isAggregate();
        }
        return false;
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
        if (offset != null)
            offset.buildingClass = buildingClass;
    }

    @Override
    public Expression resolveReference(String name) {
        // Might be a reference to the building class
        Expression reference = super.resolveReference(name);
        // Or to one of the additional FROM entities by alias
        if (reference == null && additionalFromEntities != null) {
            for (String[] entry : additionalFromEntities) {
                if (name.equals(entry[1])) {
                    Object domainClass = getModelReader().getDomainClassByName(entry[0]);
                    return new Alias(name, null, domainClass);
                }
            }
        }
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
