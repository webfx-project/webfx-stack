package dev.webfx.stack.routing.uirouter.activity.presentation.view.impl;

import dev.webfx.stack.routing.activity.ActivityContext;

/**
 * @author Bruno Salmon
 */
public final class PresentationViewActivityContextFinal<PM>
        extends PresentationViewActivityContextBase<PresentationViewActivityContextFinal<PM>, PM> {

    public PresentationViewActivityContextFinal(ActivityContext parentContext) {
        super(parentContext, PresentationViewActivityContextFinal::new);
    }

}
