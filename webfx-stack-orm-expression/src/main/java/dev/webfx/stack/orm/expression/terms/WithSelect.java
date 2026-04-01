package dev.webfx.stack.orm.expression.terms;

import java.util.List;

/**
 * Represents a DQL statement with a WITH clause (Common Table Expressions).
 * Syntax: with alias1 as (select ...), alias2 as (select ...) select ...
 *
 * @author Bruno Salmon
 */
public final class WithSelect<T> extends DqlStatement<T> {

    /** Each entry is {alias (String), cteSelect (Select)} */
    private final List<Object[]> ctes;
    private final Select<T> mainSelect;

    public WithSelect(List<Object[]> ctes, Select<T> mainSelect) {
        super(null, mainSelect.getDomainClass(), mainSelect.getDomainClassAlias(),
              mainSelect.getDefinition(), mainSelect.getSqlDefinition(),
              mainSelect.getParameterValues(),
              mainSelect.getWhere(), mainSelect.getOrderBy(), mainSelect.getLimit());
        this.ctes = ctes;
        this.mainSelect = mainSelect;
    }

    public List<Object[]> getCtes() {
        return ctes;
    }

    public Select<T> getMainSelect() {
        return mainSelect;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append("with ");
        boolean first = true;
        for (Object[] cte : ctes) {
            if (!first) sb.append(", ");
            sb.append(cte[0]).append(" as (");
            ((Select<?>) cte[1]).toString(sb);
            sb.append(')');
            first = false;
        }
        sb.append(' ');
        return mainSelect.toString(sb);
    }
}
