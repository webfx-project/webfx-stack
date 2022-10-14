package dev.webfx.stack.i18n.spi.impl.json;


import dev.webfx.platform.json.JsonObject;
import dev.webfx.stack.i18n.spi.impl.I18nProviderImpl;
import dev.webfx.stack.ui.fxraiser.FXValueRaiser;
import dev.webfx.stack.ui.fxraiser.impl.ValueConverterRegistry;
import javafx.scene.shape.SVGPath;

import static dev.webfx.platform.util.Objects.isAssignableFrom;

/**
 * @author Bruno Salmon
 */
public class JsonI18nProvider extends I18nProviderImpl {

    public JsonI18nProvider(String resourcePathWithLangPattern) {
        this(resourcePathWithLangPattern, null);
    }

    public JsonI18nProvider(String resourcePathWithLangPattern, Object defaultLanguage) {
        this(resourcePathWithLangPattern, defaultLanguage, null);
    }

    public JsonI18nProvider(String resourcePathWithLangPattern, Object defaultLanguage, Object initialLanguage) {
        super(new ResourceJsonDictionaryLoader(resourcePathWithLangPattern), defaultLanguage, initialLanguage);
    }

    static {
        ValueConverterRegistry.registerValueConverter(new FXValueRaiser() {
            @Override
            public <T> T raiseValue(Object value, Class<T> raisedClass, Object... args) {
                if (value instanceof JsonObject && isAssignableFrom(raisedClass, SVGPath.class))
                    return (T) JsonSVGPath.createSVGPath((JsonObject) value);
                return null;
            }
        });
    }

}
