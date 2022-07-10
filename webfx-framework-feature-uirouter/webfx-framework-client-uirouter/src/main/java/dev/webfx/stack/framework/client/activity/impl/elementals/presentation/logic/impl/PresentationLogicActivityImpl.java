package dev.webfx.stack.framework.client.activity.impl.elementals.presentation.logic.impl;

import dev.webfx.platform.shared.util.function.Factory;

/**
 * @author Bruno Salmon
 */
public abstract class PresentationLogicActivityImpl<PM>
        extends PresentationLogicActivityBase<PresentationLogicActivityContextFinal<PM>, PM> {

    public PresentationLogicActivityImpl() {
    }

    public PresentationLogicActivityImpl(Factory<PM> presentationModelFactory) {
        super(presentationModelFactory);
    }
}
