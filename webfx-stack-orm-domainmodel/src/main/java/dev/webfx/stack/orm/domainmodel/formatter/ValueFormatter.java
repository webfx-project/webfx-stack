package dev.webfx.stack.orm.domainmodel.formatter;

import dev.webfx.extras.type.Type;

import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public interface ValueFormatter {

    Type getFormattedValueType();

    Object formatValue(Object value);

    static ValueFormatter of(Type type, Function<Object, ?> formatFunction) {
        return new ValueFormatter() {
            @Override
            public Type getFormattedValueType() {
                return type;
            }

            @Override
            public Object formatValue(Object value) {
                return formatFunction.apply(value);
            }
        };
    }
}
