package dev.webfx.stack.framework.client.activity.impl.elementals.presentation.view.impl;

import dev.webfx.stack.framework.client.activity.ActivityContext;

/**
 * @author Bruno Salmon
 */
public final class PresentationViewActivityContextFinal<PM>
        extends PresentationViewActivityContextBase<PresentationViewActivityContextFinal<PM>, PM> {

    public PresentationViewActivityContextFinal(ActivityContext parentContext) {
        super(parentContext, PresentationViewActivityContextFinal::new);
    }

}
