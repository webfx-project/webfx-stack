package dev.webfx.stack.framework.shared.orm.expression.terms.function;

import dev.webfx.stack.framework.shared.orm.expression.lci.DomainReader;
import dev.webfx.stack.framework.shared.orm.expression.terms.Alias;
import dev.webfx.extras.type.Type;

/**
 * @author Bruno Salmon
 */
public final class ArgumentAlias extends Alias {

    private final int index;

    public ArgumentAlias(String name, Type type, int index) {
        super(name, type);
        this.index = index;
    }

    @Override
    public Object evaluate(Object data, DomainReader domainReader) {
        return getArgument();
    }

    public Object getArgument() {
        return ThreadLocalArgumentStack.getArgument(index);
    }
}
