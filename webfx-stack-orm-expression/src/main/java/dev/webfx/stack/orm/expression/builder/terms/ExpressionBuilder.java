package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.builder.BuilderThreadContext;
import dev.webfx.stack.orm.expression.parser.lci.ParserDomainModelReader;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.builder.BuilderThreadContext;
import dev.webfx.stack.orm.expression.parser.lci.ParserDomainModelReader;

/**
 * @author Bruno Salmon
 */
public abstract class ExpressionBuilder {

    public String buildingClassName;
    public Object buildingClass;

    public abstract Expression build();

    protected void propagateDomainClasses() {
        if (buildingClass == null && buildingClassName != null)
            buildingClass = getModelReader().getDomainClassByName(buildingClassName);
    }

    protected static ParserDomainModelReader getModelReader() {
        BuilderThreadContext context = BuilderThreadContext.getInstance();
        return context == null ? null : context.getModelReader();
    }
}
