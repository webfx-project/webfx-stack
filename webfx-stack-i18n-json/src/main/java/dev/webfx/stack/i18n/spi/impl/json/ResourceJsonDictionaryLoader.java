package dev.webfx.stack.i18n.spi.impl.json;

import dev.webfx.stack.i18n.Dictionary;
import dev.webfx.stack.i18n.spi.impl.DictionaryLoader;
import dev.webfx.stack.async.Promise;
import dev.webfx.platform.resource.Resource;
import dev.webfx.platform.util.Strings;
import dev.webfx.stack.async.Future;

import java.util.Set;

/**
 * @author Bruno Salmon
 */
final class ResourceJsonDictionaryLoader implements DictionaryLoader {

    private final String jsonResourcePathWithLangPattern;

    ResourceJsonDictionaryLoader(String jsonResourcePathWithLangPattern) {
        this.jsonResourcePathWithLangPattern = jsonResourcePathWithLangPattern;
    }

    private String getDictionaryResourcePath(Object lang) {
        return Strings.replaceAll(jsonResourcePathWithLangPattern, "{lang}", Strings.toString(lang));
    }

    @Override
    public Future<Dictionary> loadDictionary(Object lang, Set keys) {
        Promise<Dictionary> promise = Promise.promise();
        Resource.loadText(getDictionaryResourcePath(lang), json -> promise.complete(new JsonDictionary(json)), promise::fail);
        return promise.future();
    }
}
