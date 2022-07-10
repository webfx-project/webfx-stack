package dev.webfx.stack.framework.client.activity.impl.elementals.presentation.impl;

import dev.webfx.stack.framework.client.activity.impl.elementals.presentation.logic.impl.PresentationLogicActivityContextFinal;
import dev.webfx.stack.framework.client.activity.impl.elementals.presentation.view.impl.PresentationViewActivityContextFinal;
import dev.webfx.stack.framework.client.activity.ActivityContext;
import dev.webfx.stack.framework.client.activity.ActivityContextFactory;

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
