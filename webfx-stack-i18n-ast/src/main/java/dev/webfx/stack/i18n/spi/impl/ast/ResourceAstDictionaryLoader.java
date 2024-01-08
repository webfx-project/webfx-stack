package dev.webfx.stack.i18n.spi.impl.ast;

import dev.webfx.stack.i18n.Dictionary;
import dev.webfx.stack.i18n.spi.impl.DictionaryLoader;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.resource.Resource;
import dev.webfx.platform.util.Strings;
import dev.webfx.platform.async.Future;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
        AtomicInteger failureCounter = new AtomicInteger();
        for (String format : supportedFormats) {
            Resource.loadText(getDictionaryResourcePath(lang, format), text -> {
                if (text != null)
                    promise.tryComplete(new AstDictionary(text, format));
            }, e -> {
                if (failureCounter.incrementAndGet() == supportedFormats.length)
                    promise.fail(e);
            });
        }
        return promise.future();
    }
}
