package dev.webfx.stack.orm.domainmodel.formatter;

import dev.webfx.extras.type.DerivedType;
import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.type.Type;

/**
 * @author Bruno Salmon
 */
public final class GenericFormatterFactory {

    public static ValueFormatter createGenericFormatter(Type type) {
        String formatterName = null;
        // If the type is a derived type (ex: field with type Price), we try to find a formatter with same name (but lowercase). Ex: price
        if (type == PrimType.DATE)
            formatterName = "dateTime";
        else if (type instanceof DerivedType)
            formatterName = ((DerivedType) type).getName().toLowerCase();
        return FormatterRegistry.getFormatter(formatterName);
    }

}
