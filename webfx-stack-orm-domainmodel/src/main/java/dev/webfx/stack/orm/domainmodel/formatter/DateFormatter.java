package dev.webfx.stack.orm.domainmodel.formatter;

import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.type.Type;
import dev.webfx.platform.util.Strings;
import dev.webfx.platform.util.time.Times;

import java.time.LocalDate;


/**
 * @author Bruno Salmon
 */
public final class DateFormatter implements ValueFormatter, ValueParser {

    public static final DateFormatter SINGLETON = new DateFormatter();

    private DateFormatter() {
    }

    @Override
    public Type getFormattedValueType() {
        return PrimType.STRING;
    }

    @Override
    public Object formatValue(Object value) {
        return Times.format(value, "dd/MM/yyyy");
    }

    @Override
    public Object parseValue(Object value) {
        String text = Strings.toSafeString(value);
        if (text.isEmpty())
            return null;
        int p;
        int dayOfMonth = Integer.parseInt(text.substring(0, p = text.indexOf('/')));
        int month = Integer.parseInt(text.substring(p + 1, p = text.indexOf('/', p + 1)));
        int year = Integer.parseInt(text.substring(p + 1, p + 5));
        return LocalDate.of(year, month, dayOfMonth);
    }
}
