package dev.webfx.stack.orm.expression.terms.function.java;

import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.stack.orm.expression.terms.function.Function;

/**
 * @author Bruno Salmon
 */
public final class Coalesce extends Function {

    public Coalesce() {
        super("coalesce", null, null, null, true);
    }

    @Override
    public Object evaluate(Object argument, DomainReader domainReader) {
        if (!(argument instanceof Object[]))
            return argument;
        for (Object arg : ((Object[]) argument)) {
            if (arg != null /* && !(arg instanceof UnknownValue) */)
                return arg;
        }
        return null;
    }
}
