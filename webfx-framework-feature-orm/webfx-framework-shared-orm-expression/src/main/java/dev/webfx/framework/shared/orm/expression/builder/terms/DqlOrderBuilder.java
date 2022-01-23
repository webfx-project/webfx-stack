package dev.webfx.framework.shared.orm.expression.builder.terms;

import dev.webfx.framework.shared.orm.expression.Expression;
import dev.webfx.framework.shared.orm.expression.builder.BuilderThreadContext;
import dev.webfx.framework.shared.orm.expression.builder.ReferenceResolver;
import dev.webfx.framework.shared.orm.expression.builder.ThreadLocalReferenceResolver;
import dev.webfx.framework.shared.orm.expression.parser.lci.ParserDomainModelReader;
import dev.webfx.framework.shared.orm.expression.terms.Alias;
import dev.webfx.framework.shared.orm.expression.terms.DqlStatement;

/**
 * @author Bruno Salmon
 */
public abstract class DqlOrderBuilder<S extends DqlStatement> implements ReferenceResolver {

    public String definition;
    public String buildingClassName;
    public Object buildingClass;
    public String buildingClassAlias;
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
        if (buildingClass == null && buildingClassName != null)
            buildingClass = getModelReader().getDomainClassByName(buildingClassName);
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
        Expression reference = getModelReader().getDomainFieldSymbol(buildingClass, name);
        if (reference == null && name.equals(buildingClassAlias))
            return new Alias(name, null, buildingClass);
        return reference;
    }
}
