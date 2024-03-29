package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.platform.util.Booleans;
import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.orm.expression.lci.DomainWriter;
import dev.webfx.stack.orm.expression.Expression;

/**
 * @author Bruno Salmon
 */
public final class And<T> extends BinaryBooleanExpression<T> {

    public And(Expression<T> left, Expression<T> right) {
        super(left, " and ", right, 3);
    }

    /**
     * A shortcut value for the And operator is a value that will make the operator return false whatever the other operand is.
     * A strict And operator would accept only false as shortcut value but since this operator is not strict, it accepts
     * false and 0 as a shortcut values.
     */
    public boolean isShortcutValue(Object value) {
        return value == null || Booleans.isFalse(value) || Numbers.isZero(value);
    }

    @Override
    public Boolean evaluateCondition(Object a, Object b, DomainReader<T> domainReader) {
        if (isShortcutValue(a) || isShortcutValue(b))
            return false;
        if (a == null || b == null)
            return null;
        return true;
    }

    @Override
    public void setValue(T domainObject, Object value, DomainWriter<T> dataWriter) {
        left.setValue(domainObject, value, dataWriter);
        right.setValue(domainObject, value, dataWriter);
    }
}
