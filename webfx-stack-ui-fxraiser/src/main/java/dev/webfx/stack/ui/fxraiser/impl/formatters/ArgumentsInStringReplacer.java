package dev.webfx.stack.ui.fxraiser.impl.formatters;

import dev.webfx.stack.ui.fxraiser.impl.NamedArgument;
import dev.webfx.stack.ui.fxraiser.impl.ValueFormatter;

/**
 * @author Bruno Salmon
 */
public class ArgumentsInStringReplacer implements ValueFormatter {

    @Override
    public <T> Object formatValue(Object value, Class<T> raisedClass, Object... args) {
        if (args.length == 0 || !(value instanceof String))
            return value;
        String text = (String) value;
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            String pattern;
            if (arg instanceof NamedArgument) {
                NamedArgument namedArgument = (NamedArgument) arg;
                pattern = "{" + namedArgument.getName() + "}";
                arg = namedArgument.getArgument();
            } else
                pattern = "{" + i + "}";
            text = text.replace(pattern, String.valueOf(arg));
        }
        return text;
    }
}
