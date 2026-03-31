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
    private final List<Object[]> additionalFromEntities; // each entry: [domainClass, alias]

    public Select(Object id, Object domainClass, String domainClassAlias, String definition, String sqlDefinition, Object[] sqlParameters, boolean distinct, ExpressionArray<T> fields, Expression<T> where, ExpressionArray<T> groupBy, Expression<T> having, ExpressionArray<T> orderBy, Expression<T> limit, Expression<T> offset, boolean includeIdColumn, boolean useRowNumberAsId) {
        this(id, domainClass, domainClassAlias, definition, sqlDefinition, sqlParameters, distinct, fields, where, groupBy, having, orderBy, limit, offset, includeIdColumn, useRowNumberAsId, null);
    }

    public Select(Object id, Object domainClass, String domainClassAlias, String definition, String sqlDefinition, Object[] sqlParameters, boolean distinct, ExpressionArray<T> fields, Expression<T> where, ExpressionArray<T> groupBy, Expression<T> having, ExpressionArray<T> orderBy, Expression<T> limit, Expression<T> offset, boolean includeIdColumn, boolean useRowNumberAsId, List<Object[]> additionalFromEntities) {
        super(id, domainClass, domainClassAlias, definition, sqlDefinition, sqlParameters, where, orderBy, limit);
        this.distinct = distinct;
        this.includeIdColumn = includeIdColumn;
        this.useRowNumberAsId = useRowNumberAsId;
        this.fields = fields;
        this.groupBy = groupBy;
        this.having = having;
        this.offset = offset;
        this.additionalFromEntities = additionalFromEntities;
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
        return sb.append("select ")
                .append(_if(distinct, "distinct "))
                .append(_if(fields, " from ", sb))
                .append(_ifNotEmpty(getDomainClass(), sb)).append(_if(" ", domainClassAlias, "", sb))
                .append(_if(" where ", where, sb))
                .append(_if(" group by ", groupBy, sb))
                .append(_if(" having ", having, sb))
                .append(_if(" order by ", orderBy, sb))
                .append(_if(" limit ", limit, sb))
                .append(_if(" offset ", offset, sb));
    }
}
