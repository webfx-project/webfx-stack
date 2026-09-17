package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.builder.ThreadLocalReferenceResolver;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;
import dev.webfx.stack.orm.expression.terms.Select;
import dev.webfx.stack.orm.expression.terms.Union;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for Union (select ... union [all] select ... [order by ...]).
 *
 * @author Bruno Salmon
 */
public final class UnionBuilder {

    public String definition;
    private final SelectBuilder firstSelectBuilder;
    /** Each entry: {unionAll (Boolean), SelectBuilder} */
    private final List<Object[]> unionBuilders = new ArrayList<>();
    public ExpressionArrayBuilder orderBy; // union-level order by (applies to the whole union result)

    public UnionBuilder(SelectBuilder firstSelectBuilder) {
        this.firstSelectBuilder = firstSelectBuilder;
    }

    public void addUnion(SelectBuilder branchBuilder, boolean unionAll) {
        unionBuilders.add(new Object[]{unionAll, branchBuilder});
    }

    public Union<?> build() {
        Select<?> firstSelect = firstSelectBuilder.build();
        List<Object[]> unions = new ArrayList<>(unionBuilders.size());
        for (Object[] entry : unionBuilders) {
            Select<?> branchSelect = ((SelectBuilder) entry[1]).build();
            // All branches must target the same domain class — the first branch's query mapping is
            // applied to every row of the union result, so mixing classes would map rows to wrong
            // entities (and mix primary keys from different tables)
            if (!Objects.equals(branchSelect.getDomainClass(), firstSelect.getDomainClass()))
                throw new IllegalArgumentException("All branches of a union must select from the same domain class (found '" + branchSelect.getDomainClass() + "' after '" + firstSelect.getDomainClass() + "')");
            unions.add(new Object[]{entry[0], branchSelect});
        }
        // The union-level order by is built in the FIRST branch's context: its keys resolve against
        // the first branch's domain class and select-list 'as' aliases (SQL only allows output
        // columns in a set-operation order by, and the union's output columns are the first
        // branch's — all branches must produce the same column list anyway)
        ExpressionArray<?> builtOrderBy = null;
        if (orderBy != null) {
            orderBy.buildingClass = firstSelectBuilder.buildingClass;
            ThreadLocalReferenceResolver.pushReferenceResolver(firstSelectBuilder);
            try {
                builtOrderBy = orderBy.build();
            } finally {
                ThreadLocalReferenceResolver.popReferenceResolver();
            }
        }
        //noinspection unchecked,rawtypes
        return new Union(definition, (Select) firstSelect, unions, (ExpressionArray) builtOrderBy);
    }
}
