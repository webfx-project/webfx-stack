package dev.webfx.stack.orm.domainmodel.formatter;

import dev.webfx.extras.type.Type;

/**
 * @author Bruno Salmon
 */
public interface ValueFormatter {

    Type getFormattedValueType();

    Object formatValue(Object value);
}
