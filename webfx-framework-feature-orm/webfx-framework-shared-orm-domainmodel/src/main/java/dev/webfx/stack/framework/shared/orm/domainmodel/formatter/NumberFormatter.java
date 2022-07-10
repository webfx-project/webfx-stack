package dev.webfx.stack.framework.shared.orm.domainmodel.formatter;

import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.type.Type;

public class NumberFormatter implements ValueFormatter, ValueParser {

    public static final NumberFormatter SINGLETON = new NumberFormatter();

    private NumberFormatter() {
    }

    @Override
    public Type getFormattedValueType() {
        return PrimType.STRING;
    }

    @Override
    public Object formatValue(Object value) {
        return value == null ? "" : value.toString();
    }

    @Override
    public Object parseValue(Object value) {
        if (value == null)
            return null;
        String s = value.toString();
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
