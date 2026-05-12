package dev.webfx.stack.orm.expression.builder.terms;


import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.builder.ReferenceResolver;
import dev.webfx.stack.orm.expression.builder.ThreadLocalReferenceResolver;
import dev.webfx.stack.orm.expression.terms.*;
import dev.webfx.stack.orm.expression.terms.function.Call;
import dev.webfx.stack.orm.expression.terms.function.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Bruno Salmon
 */
public final class SelectBuilder extends DqlStatementBuilder<Select> {
    public Object filterId;
    public boolean distinct = false;
    public boolean includeIdColumn = true;
    public boolean useRowNumberAsId;
    public List<String[]> additionalFromEntities; // each entry: [className, alias]
    private Map<String, Object> cteAliasesToDomainClass; // CTE alias → resolved domain class
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

    // Lateral subqueries: alias → SelectBuilder
    public List<Object[]> lateralSubqueries; // each entry: [alias, SelectBuilder]
    private Map<String, Set<String>> lateralFieldAliasNames; // lateral alias → set of field alias names

    public void addLateralSubquery(String alias, SelectBuilder subquery) {
        if (lateralSubqueries == null)
            lateralSubqueries = new ArrayList<>();
        lateralSubqueries.add(new Object[]{alias, subquery});
    }

    public void addCteAlias(String cteAlias, Object domainClass) {
        if (cteAliasesToDomainClass == null)
            cteAliasesToDomainClass = new HashMap<>();
        cteAliasesToDomainClass.put(cteAlias, domainClass);
    }

    @Override
    protected Select buildDqlOrder() {
        propagateDomainClasses();
        // Index lateral subquery field aliases BEFORE building fields, so expressions
        // like "lat.avail" can be resolved during field building
        if (lateralSubqueries != null) {
            for (Object[] entry : lateralSubqueries) {
                String alias = (String) entry[0];
                SelectBuilder subqueryBuilder = (SelectBuilder) entry[1];
                if (subqueryBuilder.fields != null) {
                    for (ExpressionBuilder fieldBuilder : subqueryBuilder.fields.expressions) {
                        if (fieldBuilder instanceof AsBuilder) {
                            if (lateralFieldAliasNames == null)
                                lateralFieldAliasNames = new HashMap<>();
                            lateralFieldAliasNames.computeIfAbsent(alias, k -> new HashSet<>())
                                    .add(((AsBuilder) fieldBuilder).alias);
                        }
                    }
                }
            }
        }
        ExpressionArray fieldsArray = fields == null ? null : fields.build();
        if (groupBy == null && containsAggregateFunction(fieldsArray)) {
            useRowNumberAsId = true;
        }
        // Resolve additional FROM entity class names to domain class objects
        List<Object[]> resolvedAdditionalEntities = null;
        if (additionalFromEntities != null) {
            resolvedAdditionalEntities = new ArrayList<>(additionalFromEntities.size());
            for (String[] entry : additionalFromEntities) {
                String className = entry[0];
                String alias = entry[1] != null ? entry[1] : className; // default alias = class/CTE name
                Object domainClass = getModelReader().getDomainClassByName(className);
                if (domainClass == null) {
                    // Try explicit CTE alias map first, then fall back to thread-local resolver (for chained CTEs)
                    Object cteDomainClass = (cteAliasesToDomainClass != null) ? cteAliasesToDomainClass.get(className) : null;
                    if (cteDomainClass == null) {
                        Expression ref = ThreadLocalReferenceResolver.resolveReference(className);
                        if (ref instanceof Alias)
                            cteDomainClass = ((Alias<?>) ref).getDomainClass();
                    }
                    if (cteDomainClass != null) {
                        // {domainClass, sqlAlias, cteName} — cteName used as SQL "table name"
                        resolvedAdditionalEntities.add(new Object[]{cteDomainClass, alias, className});
                        continue;
                    }
                }
                resolvedAdditionalEntities.add(new Object[]{domainClass, alias});
            }
        }
        // Build lateral subqueries: each entry becomes [alias, Select]
        List<Object[]> builtLaterals = null;
        if (lateralSubqueries != null) {
            builtLaterals = new ArrayList<>(lateralSubqueries.size());
            for (Object[] entry : lateralSubqueries) {
                String alias = (String) entry[0];
                SelectBuilder subqueryBuilder = (SelectBuilder) entry[1];
                // Don't set buildingClass — lateral subqueries without FROM should not
                // generate a FROM clause. References resolve through the thread-local stack.
                builtLaterals.add(new Object[]{alias, subqueryBuilder.build()});
            }
        }
        Select<?> select = new Select<>(filterId, buildingClass, buildingClassAlias, definition, sqlDefinition, sqlParameters,
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
            resolvedAdditionalEntities,
            builtLaterals
            );
        if (buildingClassCteAlias != null)
            select.setDomainClassCteAlias(buildingClassCteAlias);
        return select;
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
    public Expression resolveDotReference(String aliasName, String fieldName) {
        if (lateralFieldAliasNames == null) return null;
        Set<String> fields = lateralFieldAliasNames.get(aliasName);
        return (fields != null && fields.contains(fieldName)) ? new CteColumnRef<>(aliasName, fieldName) : null;
    }

    @Override
    public Expression resolveReference(String name) {
        // Might be a reference to the building class
        Expression reference = super.resolveReference(name);
        // Or to one of the additional FROM entities by alias
        if (reference == null && additionalFromEntities != null) {
            for (String[] entry : additionalFromEntities) {
                String effectiveAlias = entry[1] != null ? entry[1] : entry[0]; // default alias = class/CTE name
                if (name.equals(effectiveAlias)) {
                    Object domainClass = getModelReader().getDomainClassByName(entry[0]);
                    if (domainClass == null && cteAliasesToDomainClass != null)
                        domainClass = cteAliasesToDomainClass.get(entry[0]);
                    return new Alias(name, null, domainClass);
                }
            }
        }
        // Or to a lateral subquery alias
        if (reference == null && lateralSubqueries != null) {
            for (Object[] entry : lateralSubqueries) {
                String alias = (String) entry[0];
                if (name.equals(alias)) {
                    // Resolve lateral alias; domain class = parent's building class
                    return new Alias(name, null, buildingClass);
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
