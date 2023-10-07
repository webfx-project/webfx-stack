package dev.webfx.stack.i18n.spi.impl.ast;


import dev.webfx.stack.i18n.spi.impl.I18nProviderImpl;

/**
 * @author Bruno Salmon
 */
public class AstI18nProvider extends I18nProviderImpl {

    public AstI18nProvider() {
        this("dev/webfx/stack/i18n/{lang}.properties");
    }

    public AstI18nProvider(String resourcePathWithLangPattern) {
        this(resourcePathWithLangPattern, null);
    }

    public AstI18nProvider(String resourcePathWithLangPattern, Object defaultLanguage) {
        this(resourcePathWithLangPattern, defaultLanguage, null);
    }

    public AstI18nProvider(String resourcePathWithLangPattern, Object defaultLanguage, Object initialLanguage) {
        super(new ResourceAstDictionaryLoader(resourcePathWithLangPattern), defaultLanguage, initialLanguage);
    }

}
