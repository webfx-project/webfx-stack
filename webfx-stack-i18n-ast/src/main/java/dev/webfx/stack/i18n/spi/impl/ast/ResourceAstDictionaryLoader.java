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
    private final String[] supportedFormats;

    ResourceAstDictionaryLoader(String astResourcePathWithLangPattern, String... supportedFormats) {
        this.astResourcePathWithLangPattern = astResourcePathWithLangPattern;
        this.supportedFormats = supportedFormats;
    }

    private String getDictionaryResourcePath(Object lang, String format) {
        String path = astResourcePathWithLangPattern;
        path = Strings.replaceAll(path, "{lang}", Strings.toString(lang));
        path = Strings.replaceAll(path, "{format}", Strings.toString(format));
        return path;
    }

    @Override
    public Future<Dictionary> loadDictionary(Object lang, Set<Object> keys) {
        Promise<Dictionary> promise = Promise.promise();
        Throwable[] failure = { null };
        for (String format : supportedFormats) {
            Resource.loadText(getDictionaryResourcePath(lang, format), text -> {
                if (text != null)
                    promise.tryComplete(new AstDictionary(text, format));
            }, e -> failure[0] = e);
        }
        // Raising error if promise not complete and failure occurred TODO: improve this for real async loading
        if (failure[0] != null)
            promise.tryFail(failure[0]);
        return promise.future();
    }
}
