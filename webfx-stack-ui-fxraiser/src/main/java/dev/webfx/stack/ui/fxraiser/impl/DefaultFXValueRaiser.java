package dev.webfx.stack.ui.fxraiser.impl;

import dev.webfx.platform.util.Objects;
import dev.webfx.stack.ui.fxraiser.FXValueRaiser;
import javafx.beans.value.ObservableValue;

import java.util.Arrays;

/**
 * @author Bruno Salmon
 */
public class DefaultFXValueRaiser implements FXValueRaiser {

    @Override
    public <T> T raiseValue(Object value, Class<T> raisedClass, Object... args) {
        // Extracting the final value when a property is passed
        value = getValueOrPropertyValue(value);
        // Same extraction for the arguments
        args = Arrays.stream(args).map(DefaultFXValueRaiser::getValueOrPropertyValue).toArray();
        // Formatting the value with the passed arguments (see for example ArgumentsInStringReplacer)
        // Note: ArgumentsInStringReplacer will not process special values such as I18nProviderImpl.TokenSnapshot instances
        Object formattedValue = ValueFormatterRegistry.formatValue(value, raisedClass, args);
        // Converting the value to the raisedClass, if possible.
        // Note: this is at this point that I18nProviderImpl.TokenSnapshot will be converted (into String for example)
        T convertedValue = ValueConverterRegistry.convertValue(formattedValue, raisedClass, args);
        // If the value has not been formatted in the previous pass, but has been converted (ex: I18nProviderImpl.TokenSnapshot)
        if (formattedValue == value && convertedValue != formattedValue) {
            // We try to format it after the conversion
            formattedValue = ValueFormatterRegistry.formatValue(convertedValue, raisedClass, args);
            // If the converted value has been formatted in this second pass, and that this formatted value is of the right class
            if (formattedValue != convertedValue && Objects.isInstanceOf(formattedValue, raisedClass))
                convertedValue = (T) formattedValue; // we accept this formatted value as the value to return
        }
        return convertedValue;
    }

    public static Object getValueOrPropertyValue(Object value) {
        if (value instanceof ObservableValue)
            value = ((ObservableValue<?>) value).getValue();
        return value;
    }
}
