package dev.webfx.stack.orm.domainmodel.formatter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class FormatterRegistry {

    private final static Map<String, ValueFormatter> formatters = new HashMap<>();

    public static void registerFormatter(String formatName, ValueFormatter formatter) {
        formatters.put(formatName, formatter);
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
