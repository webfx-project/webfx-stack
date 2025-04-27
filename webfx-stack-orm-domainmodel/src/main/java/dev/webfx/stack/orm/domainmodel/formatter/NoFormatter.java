package dev.webfx.stack.orm.domainmodel.formatter;

import dev.webfx.extras.type.Type;

/**
 * @author Bruno Salmon
 */
public final class NoFormatter implements ValueFormatter, ValueParser {

    public static final NoFormatter SINGLETON = new NoFormatter();

    private NoFormatter() {}

    @Override
    public Type getFormattedValueType() {
        return null;
    }

    @Override
    public Object formatValue(Object value) {
        return value;
    }

    @Override
    public Object parseValue(Object value) {
        return value;
    }

}
