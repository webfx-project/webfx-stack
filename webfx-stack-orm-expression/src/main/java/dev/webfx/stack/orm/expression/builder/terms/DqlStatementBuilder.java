package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.builder.BuilderThreadContext;
import dev.webfx.stack.orm.expression.builder.ReferenceResolver;
import dev.webfx.stack.orm.expression.builder.ThreadLocalReferenceResolver;
import dev.webfx.stack.orm.expression.parser.lci.ParserDomainModelReader;
import dev.webfx.stack.orm.expression.terms.Alias;
import dev.webfx.stack.orm.expression.terms.DqlStatement;

/**
 * @author Bruno Salmon
 */
public abstract class DqlStatementBuilder<S extends DqlStatement> implements ReferenceResolver {

    public String definition;
    public String buildingClassName;
    public Object buildingClass;
    public String buildingClassAlias;
    public String buildingClassCteAlias; // non-null when buildingClass was resolved via a CTE alias
    public ExpressionBuilder where;
    public ExpressionBuilder limit;
    public String sqlDefinition;
    public Object[] sqlParameters;

    public S build() {
        propagateDomainClasses();
        ThreadLocalReferenceResolver.pushReferenceResolver(this);
        S dqlOrder = buildDqlOrder();
        ThreadLocalReferenceResolver.popReferenceResolver();
        return dqlOrder;
    }

    protected void propagateDomainClasses() {
        if (buildingClass == null && buildingClassName != null) {
            buildingClass = getModelReader().getDomainClassByName(buildingClassName);
            if (buildingClass == null) {
                // Fall back to CTE alias resolution via thread-local resolver stack
                Expression ref = ThreadLocalReferenceResolver.resolveReference(buildingClassName);
                if (ref instanceof Alias) {
                    buildingClass = ((Alias<?>) ref).getDomainClass();
                    buildingClassCteAlias = buildingClassName; // remember the CTE name for SQL generation
                }
            }
        }
        if (where != null)
            where.buildingClass = buildingClass;
        if (limit != null)
            limit.buildingClass = buildingClass;

    }

    protected static ParserDomainModelReader getModelReader() {
        return BuilderThreadContext.getInstance().getModelReader();
    }


    protected abstract S buildDqlOrder();

    @Override
    public Expression resolveReference(String name) {
        if (buildingClass == null)
            return null;
        Expression reference = getModelReader().getDomainFieldSymbol(buildingClass, name);
        if (reference == null && name.equals(buildingClassAlias))
            return new Alias(name, null, buildingClass);
        return reference;
    }
}
