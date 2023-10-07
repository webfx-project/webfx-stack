package dev.webfx.stack.i18n.spi.impl.ast;

import dev.webfx.stack.i18n.Dictionary;
import dev.webfx.stack.i18n.spi.impl.DictionaryLoader;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.resource.Resource;
import dev.webfx.platform.util.Strings;
import dev.webfx.platform.async.Future;

import java.util.Set;

/**
 * @author Bruno Salmon
 */
final class ResourceAstDictionaryLoader implements DictionaryLoader {

    private final String astResourcePathWithLangPattern;

    ResourceAstDictionaryLoader(String astResourcePathWithLangPattern) {
        this.astResourcePathWithLangPattern = astResourcePathWithLangPattern;
    }

    private String getDictionaryResourcePath(Object lang) {
        return Strings.replaceAll(astResourcePathWithLangPattern, "{lang}", Strings.toString(lang));
    }

    @Override
    public Future<Dictionary> loadDictionary(Object lang, Set<Object> keys) {
        Promise<Dictionary> promise = Promise.promise();
        Resource.loadText(getDictionaryResourcePath(lang), text -> promise.complete(new AstDictionary(text, "properties")), promise::fail);
        return promise.future();
    }
}
