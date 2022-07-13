package dev.webfx.stack.routing.uirouter.activity.presentation.impl;

import dev.webfx.stack.routing.uirouter.activity.presentation.logic.impl.PresentationLogicActivityContextFinal;
import dev.webfx.stack.routing.uirouter.activity.presentation.view.impl.PresentationViewActivityContextFinal;
import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.ActivityContextFactory;

/**
 * @author Bruno Salmon
 */
public final class PresentationActivityContextFinal
       <PM>

        extends PresentationActivityContextBase<PresentationActivityContextFinal<PM>, PresentationViewActivityContextFinal<PM>, PresentationLogicActivityContextFinal<PM>, PM> {


    public PresentationActivityContextFinal(ActivityContext parentContext) {
        super(parentContext, PresentationActivityContextFinal::new);
    }

    public PresentationActivityContextFinal(ActivityContext parentContext, ActivityContextFactory<PresentationActivityContextFinal<PM>> contextFactory) {
        super(parentContext, contextFactory);
    }
}
