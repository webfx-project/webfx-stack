package dev.webfx.stack.ui.fxraiser.impl;

import dev.webfx.stack.ui.fxraiser.FXValueRaiser;
import javafx.beans.value.ObservableValue;

import java.util.Arrays;

/**
 * @author Bruno Salmon
 */
public class DefaultFXValueRaiser implements FXValueRaiser {

    @Override
    public <T> T raiseValue(Object value, Class<T> raisedClass, Object... args) {
        value = getValueOrPropertyValue(value);
        args = Arrays.stream(args).map(DefaultFXValueRaiser::getValueOrPropertyValue).toArray();
        Object formattedValue = ValueFormatterRegistry.formatValue(value, raisedClass, args);
        return ValueConverterRegistry.convertValue(formattedValue, raisedClass, args);
    }

    public static Object getValueOrPropertyValue(Object value) {
        if (value instanceof ObservableValue)
            value = ((ObservableValue<?>) value).getValue();
        return value;
    }
}
