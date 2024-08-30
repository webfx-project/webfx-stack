package dev.webfx.stack.ui.fxraiser.impl;

import dev.webfx.stack.ui.fxraiser.FXValueRaiser;
import dev.webfx.stack.ui.fxraiser.impl.converters.ImageToImageViewConverter;
import dev.webfx.stack.ui.fxraiser.impl.converters.StringToPaintConverter;
import dev.webfx.stack.ui.fxraiser.impl.converters.StringUrlToImageConverter;

import java.util.ArrayList;
import java.util.List;

import static dev.webfx.platform.util.Objects.isAssignableFrom;

/**
 * @author Bruno Salmon
 */
public class ValueConverterRegistry {

    private static final List<FXValueRaiser> VALUE_CONVERTERS = new ArrayList<>();

    static {
        registerDefaultConverters();
    }

    public static void registerValueConverter(FXValueRaiser valueConverter) {
        VALUE_CONVERTERS.add(valueConverter);
    }

    public static void registerDefaultConverters() {
        registerValueConverter(new StringUrlToImageConverter());
        registerValueConverter(new StringToPaintConverter());
        registerValueConverter(new ImageToImageViewConverter());
    }

    public static <T> T convertValue(Object value, Class<T> raisedClass, Object... args) {
        if (value == null)
            return null;
        // First choice: one converter can do the whole job
        for (FXValueRaiser valueConverter : VALUE_CONVERTERS) {
            T convertedValue = valueConverter.raiseValue(value, raisedClass, args);
            if (convertedValue != null)
                return convertedValue;
        }
        // Second choice: several converters can be chained to do the job (ex: String -> Image -> ImageView)
        if (!raisedClass.equals(Object.class))
            for (FXValueRaiser valueConverter : VALUE_CONVERTERS) {
                Object objectConvertedValue = valueConverter.raiseValue(value, Object.class, args);
                if (objectConvertedValue != null) {
                    T convertedValue = convertValue(objectConvertedValue, raisedClass, args);
                    if (convertedValue != null)
                        return convertedValue;
                }
            }
        if (isAssignableFrom(raisedClass, value.getClass()))
            return (T) value;
        // Couldn't find any matching conversion, sorry!
        return null;
    }

}
