package dev.webfx.stack.framework.client.services.i18n.spi.impl;

import dev.webfx.stack.framework.client.services.i18n.Dictionary;
import dev.webfx.stack.platform.async.Future;

import java.util.Set;

/**
 * @author Bruno Salmon
 */
public interface DictionaryLoader {

    Future<Dictionary> loadDictionary(Object lang, Set keys);

}
