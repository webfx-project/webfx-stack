package dev.webfx.stack.orm.expression.terms.function.java;

import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.stack.orm.expression.terms.function.Function;
import dev.webfx.extras.type.PrimType;
import dev.webfx.stack.orm.expression.lci.DomainReader;

import java.time.LocalDate;

/**
 * @author Bruno Salmon
 */
public final class CurrentDate extends Function {

    public CurrentDate() {
        super("current_date", PrimType.DATE, true, true);
    }

    @Override
    public Object evaluate(Object argument, DomainReader domainReader) {
        return LocalDate.now();
    }
}
