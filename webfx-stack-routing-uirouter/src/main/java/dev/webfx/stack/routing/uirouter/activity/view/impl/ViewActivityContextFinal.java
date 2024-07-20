package dev.webfx.stack.routing.uirouter.activity.view.impl;

import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.ActivityContextFactory;

/**
 * @author Bruno Salmon
 */
public final class ViewActivityContextFinal extends ViewActivityContextBase<ViewActivityContextFinal> {

    public ViewActivityContextFinal(ActivityContext parentContext, ActivityContextFactory<ViewActivityContextFinal> contextFactory) {
        super(parentContext, contextFactory);
    }

    public ViewActivityContextFinal(ActivityContext parentContext) {
        super(parentContext, ViewActivityContextFinal::new);
    }

}
