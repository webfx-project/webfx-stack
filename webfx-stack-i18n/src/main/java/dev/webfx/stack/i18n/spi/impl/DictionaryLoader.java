package dev.webfx.stack.i18n.spi.impl;

import dev.webfx.stack.i18n.Dictionary;
import dev.webfx.platform.async.Future;

import java.util.Set;

/**
 * @author Bruno Salmon
 */
public interface DictionaryLoader {

    Future<Dictionary> loadDictionary(Object lang, Set<Object> keys);

}
