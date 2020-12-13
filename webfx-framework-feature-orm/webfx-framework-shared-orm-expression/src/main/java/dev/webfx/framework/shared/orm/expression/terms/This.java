package dev.webfx.framework.shared.orm.expression.terms;

import dev.webfx.extras.type.Type;
import dev.webfx.framework.shared.orm.expression.CollectOptions;
import dev.webfx.framework.shared.orm.expression.lci.DomainReader;

/**
 * @author Bruno Salmon
 */
public final class This<T> extends AbstractExpression<T> {

    public static final This SINGLETON = new This();

    private This() {
        super(9);
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public Object evaluate(T domainObject, DomainReader<T> domainReader) {
        return domainObject;
    }

    @Override
    public void collect(CollectOptions options) {
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        return sb.append("this");
    }
}
