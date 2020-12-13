package dev.webfx.framework.client.activity.impl.elementals.view.impl;

import dev.webfx.framework.client.activity.ActivityContext;
import dev.webfx.framework.client.activity.ActivityContextFactory;

/**
 * @author Bruno Salmon
 */
public final class ViewActivityContextFinal extends ViewActivityContextBase<ViewActivityContextFinal> {

    public ViewActivityContextFinal(ActivityContext parentContext, ActivityContextFactory<ViewActivityContextFinal> contextFactory) {
        super(parentContext, contextFactory);
    }
}
