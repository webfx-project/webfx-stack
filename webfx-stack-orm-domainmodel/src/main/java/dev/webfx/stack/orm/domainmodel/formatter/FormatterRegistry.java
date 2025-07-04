package dev.webfx.stack.orm.domainmodel.formatter;

import dev.webfx.extras.type.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class FormatterRegistry {

    private final static Map<String, ValueFormatter> formatters = new HashMap<>();

    public static void registerFormatter(String formatName, ValueFormatter formatter) {
        formatters.put(formatName, formatter);
    }

    public static void registerFormatter(String formatName, Type type, Function<Object, ?> formatFunction) {
        formatters.put(formatName, ValueFormatter.of(type, formatFunction));
    }

    public static ValueFormatter getFormatter(String formatName) {
        return formatters.get(formatName);
    }

    static {
        // Registering default generic formatters
        registerFormatter("none", NoFormatter.SINGLETON); // Purpose = to prevent using GenericFormatterFactory
        registerFormatter("date", DateFormatter.SINGLETON);
        registerFormatter("dateTime", DateTimeFormatter.SINGLETON);
    }
}
