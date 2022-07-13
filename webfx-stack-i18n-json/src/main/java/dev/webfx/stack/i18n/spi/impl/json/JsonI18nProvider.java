package dev.webfx.stack.i18n.spi.impl.json;


import dev.webfx.stack.i18n.spi.impl.I18nProviderImpl;
import dev.webfx.stack.platform.json.JsonObject;

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

    @Override
    protected String findTokenValueInKey(Object i18nKey, String token) {
        if (i18nKey instanceof JsonObject)
            return ((JsonObject) i18nKey).getString(token);
        return super.findTokenValueInKey(i18nKey, token);
    }
}
