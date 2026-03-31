package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.builder.ReferenceResolver;
import dev.webfx.stack.orm.expression.builder.ThreadLocalReferenceResolver;
import dev.webfx.stack.orm.expression.terms.Alias;
import dev.webfx.stack.orm.expression.terms.Select;
import dev.webfx.stack.orm.expression.terms.WithSelect;
import dev.webfx.stack.orm.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for WithSelect (WITH clause / CTEs).
 *
 * @author Bruno Salmon
 */
public final class WithSelectBuilder implements ReferenceResolver {

    /** Each entry: {alias (String), SelectBuilder} */
    private final List<Object[]> cteBuilders = new ArrayList<>();
    private SelectBuilder mainSelectBuilder;

    public void addCte(String alias, SelectBuilder cteBuilder) {
        cteBuilders.add(new Object[]{alias, cteBuilder});
    }

    public void setMainSelect(SelectBuilder mainSelectBuilder) {
        this.mainSelectBuilder = mainSelectBuilder;
    }

    public WithSelect<?> build() {
        // Build each CTE select, collecting resolved domain-class aliases as we go
        // so the main select can reference CTE aliases
        ThreadLocalReferenceResolver.pushReferenceResolver(this);
        List<Object[]> ctes = new ArrayList<>(cteBuilders.size());
        for (Object[] entry : cteBuilders) {
            String alias = (String) entry[0];
            SelectBuilder cteBuilder = (SelectBuilder) entry[1];
            Select<?> cteSelect = cteBuilder.build();
            ctes.add(new Object[]{alias, cteSelect});
            // Register the CTE alias with the main select builder so it can resolve field references
            mainSelectBuilder.addCteAlias(alias, cteSelect.getDomainClass());
        }
        Select<?> mainSelect = mainSelectBuilder.build();
        ThreadLocalReferenceResolver.popReferenceResolver();
        //noinspection unchecked,rawtypes
        return new WithSelect(ctes, mainSelect);
    }

    @Override
    public Expression resolveReference(String name) {
        // Allow the main select's WHERE clause to reference CTE aliases
        for (Object[] entry : cteBuilders) {
            if (name.equals(entry[0])) {
                SelectBuilder cteBuilder = (SelectBuilder) entry[1];
                Object domainClass = cteBuilder.buildingClass != null ? cteBuilder.buildingClass
                        : getModelReader().getDomainClassByName(cteBuilder.buildingClassName);
                return new Alias(name, null, domainClass);
            }
        }
        return null;
    }

    private static dev.webfx.stack.orm.expression.parser.lci.ParserDomainModelReader getModelReader() {
        return dev.webfx.stack.orm.expression.builder.BuilderThreadContext.getInstance().getModelReader();
    }
}
