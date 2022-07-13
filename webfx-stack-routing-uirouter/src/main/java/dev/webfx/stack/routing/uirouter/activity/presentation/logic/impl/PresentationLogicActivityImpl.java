package dev.webfx.stack.routing.uirouter.activity.presentation.logic.impl;

import dev.webfx.platform.util.function.Factory;

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
