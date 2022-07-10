package dev.webfx.stack.framework.shared.orm.expression.terms.function.java;

import dev.webfx.stack.framework.shared.orm.expression.lci.DomainReader;
import dev.webfx.stack.framework.shared.orm.expression.terms.function.Function;
import dev.webfx.extras.type.PrimType;

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
