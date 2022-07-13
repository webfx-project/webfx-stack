package dev.webfx.stack.routing.uirouter.activity.presentation.logic.impl;

import dev.webfx.stack.routing.activity.ActivityContext;

/**
 * @author Bruno Salmon
 */
public final class PresentationLogicActivityContextFinal<PM>
        extends PresentationLogicActivityContextBase<PresentationLogicActivityContextFinal<PM>, PM> {

    public PresentationLogicActivityContextFinal(ActivityContext parentContext) {
        super(parentContext, PresentationLogicActivityContextFinal::new);
    }
}
