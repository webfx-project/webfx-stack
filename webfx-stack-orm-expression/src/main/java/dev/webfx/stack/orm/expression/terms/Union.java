package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.CollectOptions;

import java.util.List;

/**
 * Represents a DQL union of several selects over the SAME domain class.
 * Syntax: select ... union [all] select ... [union [all] select ...] [order by ...]
 *
 * All branches must target the same domain class and produce the same column list — the query
 * mapping of the first branch is used to map every row of the union result back to entities.
 * A trailing "order by" is the UNION-LEVEL order by (SQL-standard semantics): it applies to the
 * whole union result, and its keys must reference selected columns of the first branch (either
 * by repeating the select expression or by naming its 'as' alias) — SQL restricts a set-operation
 * order by to output columns. A branch needing its own order by/limit must be parenthesized:
 * (select ... order by ... limit ...) union ... — such branches are compiled parenthesized.
 *
 * @author Bruno Salmon
 */
public final class Union<T> extends DqlStatement<T> {

    private final Select<T> firstSelect;
    /** Each entry is {unionAll (Boolean), branchSelect (Select)} — branches unioned after the first */
    private final List<Object[]> unions;

    public Union(String definition, Select<T> firstSelect, List<Object[]> unions, ExpressionArray<T> orderBy) {
        // where/limit are per-branch for a union, so none is exposed at the statement level
        // (exposing the first branch's would mislead consumers such as push partition scoping);
        // orderBy however IS union-level, so it is exposed as the statement's orderBy
        super(null, firstSelect.getDomainClass(), firstSelect.getDomainClassAlias(),
              definition, firstSelect.getSqlDefinition(), firstSelect.getParameterValues(),
              null, orderBy, null);
        this.firstSelect = firstSelect;
        this.unions = unions;
    }

    public Select<T> getFirstSelect() {
        return firstSelect;
    }

    public List<Object[]> getUnions() {
        return unions;
    }

    @Override
    public void collect(CollectOptions options) {
        firstSelect.collect(options);
        for (Object[] union : unions)
            ((Select<?>) union[1]).collect(options);
        if (orderBy != null)
            orderBy.collect(options);
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        firstSelect.toString(sb);
        for (Object[] union : unions) {
            sb.append((Boolean) union[0] ? " union all " : " union ");
            ((Select<?>) union[1]).toString(sb);
        }
        if (orderBy != null)
            orderBy.toString(sb.append(" order by "));
        return sb;
    }
}
