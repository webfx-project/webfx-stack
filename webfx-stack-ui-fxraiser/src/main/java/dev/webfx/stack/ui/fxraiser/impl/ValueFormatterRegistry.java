package dev.webfx.stack.ui.fxraiser.impl;

import dev.webfx.stack.ui.fxraiser.impl.formatters.ArgumentsInStringReplacer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public class ValueFormatterRegistry {

    private static final List<ValueFormatter> VALUE_FORMATTERS = new ArrayList<>();

    static {
        registerDefaultFormatters();
    }

    private static void registerValueFormatter(ValueFormatter valueFormatter) {
        VALUE_FORMATTERS.add(valueFormatter);
    }

    public static void registerDefaultFormatters() {
        registerValueFormatter(new ArgumentsInStringReplacer());
    }

    public static <T> Object formatValue(Object value, Class<T> raisedClass, Object... args) {
        for (ValueFormatter valueFormatter : VALUE_FORMATTERS) {
            Object formatValue = valueFormatter.formatValue(value, raisedClass, args);
            if (formatValue != null && formatValue != value)
                return formatValue(formatValue, raisedClass, args);
        }
        return value;
    }
}
