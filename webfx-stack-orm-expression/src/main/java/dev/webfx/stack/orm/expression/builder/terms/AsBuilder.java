package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.builder.ReferenceResolver;
import dev.webfx.stack.orm.expression.terms.Alias;
import dev.webfx.stack.orm.expression.terms.As;
import dev.webfx.stack.orm.expression.terms.Symbol;

/**
 * @author Bruno Salmon
 */
public final class AsBuilder extends UnaryExpressionBuilder implements ReferenceResolver {
    public final String alias;

    public AsBuilder(ExpressionBuilder operand, String alias) {
        super(operand);
        this.alias = alias;
    }

    @Override
    protected As newUnaryOperation(Expression operand) {
        return new As(operand, alias);
    }

    @Override
    public Expression resolveReference(String name) {
        if (!name.equals(alias))
            return null;
        Expression build = operand.build();
        Object domainClass = buildingClass;
        if (build instanceof Symbol)
            domainClass = getModelReader().getSymbolForeignDomainClass(buildingClass, (Symbol) build);
        return new Alias(alias, build.getType(), domainClass);
    }
}
