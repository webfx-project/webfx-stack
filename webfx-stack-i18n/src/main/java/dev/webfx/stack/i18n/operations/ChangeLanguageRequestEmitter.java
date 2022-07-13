package dev.webfx.stack.i18n.operations;

import dev.webfx.platform.util.collection.Collections;

import java.util.Collection;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public interface ChangeLanguageRequestEmitter {

    ChangeLanguageRequest emitLanguageRequest();

    static Collection<ChangeLanguageRequestEmitter> getProvidedEmitters() {
        return Collections.listOf(ServiceLoader.load(ChangeLanguageRequestEmitter.class));
    }
}
