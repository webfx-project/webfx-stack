package dev.webfx.stack.i18n.operations;

import dev.webfx.platform.util.function.Factory;

/**
 * @author Bruno Salmon
 */
public class ChangeLanguageRequestEmitterImpl implements ChangeLanguageRequestEmitter {

    private final Factory<ChangeLanguageRequest> changeLanguageRequestFactory;

    public ChangeLanguageRequestEmitterImpl(Factory<ChangeLanguageRequest> changeLanguageRequestFactory) {
        this.changeLanguageRequestFactory = changeLanguageRequestFactory;
    }

    @Override
    public ChangeLanguageRequest emitLanguageRequest() {
        return changeLanguageRequestFactory.create();
    }
}
