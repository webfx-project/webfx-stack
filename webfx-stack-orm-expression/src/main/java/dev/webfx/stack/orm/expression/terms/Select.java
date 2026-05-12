package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.CollectOptions;
import dev.webfx.stack.orm.expression.Expression;

import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class Select<T> extends DqlStatement<T> {

    private final boolean distinct;
    private final boolean includeIdColumn;
    private final boolean useRowNumberAsId;
    private final ExpressionArray<T> fields;
    private final ExpressionArray<T> groupBy;
    private final Expression<T> having;
    private final Expression<T> offset;
    private final List<Object[]> additionalFromEntities; // each entry: [domainClass, alias] or [domainClass, alias, cteName]
    private final List<Object[]> lateralSubqueries; // each entry: [alias, Select]
    private String domainClassCteAlias; // non-null when domain class was resolved via a CTE alias

    public Select(Object id, Object domainClass, String domainClassAlias, String definition, String sqlDefinition, Object[] sqlParameters, boolean distinct, ExpressionArray<T> fields, Expression<T> where, ExpressionArray<T> groupBy, Expression<T> having, ExpressionArray<T> orderBy, Expression<T> limit, Expression<T> offset, boolean includeIdColumn, boolean useRowNumberAsId) {
        this(id, domainClass, domainClassAlias, definition, sqlDefinition, sqlParameters, distinct, fields, where, groupBy, having, orderBy, limit, offset, includeIdColumn, useRowNumberAsId, null, null);
    }

    public Select(Object id, Object domainClass, String domainClassAlias, String definition, String sqlDefinition, Object[] sqlParameters, boolean distinct, ExpressionArray<T> fields, Expression<T> where, ExpressionArray<T> groupBy, Expression<T> having, ExpressionArray<T> orderBy, Expression<T> limit, Expression<T> offset, boolean includeIdColumn, boolean useRowNumberAsId, List<Object[]> additionalFromEntities) {
        this(id, domainClass, domainClassAlias, definition, sqlDefinition, sqlParameters, distinct, fields, where, groupBy, having, orderBy, limit, offset, includeIdColumn, useRowNumberAsId, additionalFromEntities, null);
    }

    public Select(Object id, Object domainClass, String domainClassAlias, String definition, String sqlDefinition, Object[] sqlParameters, boolean distinct, ExpressionArray<T> fields, Expression<T> where, ExpressionArray<T> groupBy, Expression<T> having, ExpressionArray<T> orderBy, Expression<T> limit, Expression<T> offset, boolean includeIdColumn, boolean useRowNumberAsId, List<Object[]> additionalFromEntities, List<Object[]> lateralSubqueries) {
        super(id, domainClass, domainClassAlias, definition, sqlDefinition, sqlParameters, where, orderBy, limit);
        this.distinct = distinct;
        this.includeIdColumn = includeIdColumn;
        this.useRowNumberAsId = useRowNumberAsId;
        this.fields = fields;
        this.groupBy = groupBy;
        this.having = having;
        this.offset = offset;
        this.additionalFromEntities = additionalFromEntities;
        this.lateralSubqueries = lateralSubqueries;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public boolean isIncludeIdColumn() {
        return includeIdColumn;
    }

    public boolean isUseRowNumberAsId() {
        return useRowNumberAsId;
    }

    public ExpressionArray<T> getFields() {
        return fields;
    }

    public ExpressionArray<T> getGroupBy() {
        return groupBy;
    }

    public Expression<T> getHaving() {
        return having;
    }

    public Expression<T> getOffset() {
        return offset;
    }

    public List<Object[]> getAdditionalFromEntities() {
        return additionalFromEntities;
    }

    public List<Object[]> getLateralSubqueries() {
        return lateralSubqueries;
    }

    public String getDomainClassCteAlias() {
        return domainClassCteAlias;
    }

    public void setDomainClassCteAlias(String domainClassCteAlias) {
        this.domainClassCteAlias = domainClassCteAlias;
    }

    @Override
    public void collect(CollectOptions options) {
        if (fields != null)
            fields.collect(options);
        super.collect(options);
        if (groupBy != null)
            groupBy.collect(options);
        if (having != null)
            having.collect(options);
        if (offset != null)
            offset.collect(options);
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append("select ")
                .append(_if(distinct, "distinct "));
        _if(fields, " from ", sb);
        _ifNotEmpty(getDomainClass(), sb);
        _if(" ", domainClassAlias, "", sb);
        if (additionalFromEntities != null) {
            for (Object[] entry : additionalFromEntities) {
                sb.append(", ").append(entry[0]);
                if (entry[1] != null)
                    sb.append(' ').append(entry[1]);
            }
        }
        if (lateralSubqueries != null) {
            for (Object[] entry : lateralSubqueries) {
                String alias = (String) entry[0];
                Select<?> subquery = (Select<?>) entry[1];
                sb.append(", lateral (");
                subquery.toString(sb);
                sb.append(") ").append(alias);
            }
        }
        _if(" where ", where, sb);
        _if(" group by ", groupBy, sb);
        _if(" having ", having, sb);
        _if(" order by ", orderBy, sb);
        _if(" limit ", limit, sb);
        _if(" offset ", offset, sb);
        return sb;
    }
}
