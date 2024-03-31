package dev.webfx.stack.i18n.operations;

import dev.webfx.platform.service.MultipleServiceProviders;

import java.util.Collection;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public interface ChangeLanguageRequestEmitter {

    ChangeLanguageRequest emitLanguageRequest();

    static Collection<ChangeLanguageRequestEmitter> getProvidedEmitters() {
        return MultipleServiceProviders.getProviders(ChangeLanguageRequestEmitter.class, () -> ServiceLoader.load(ChangeLanguageRequestEmitter.class));
    }
}
