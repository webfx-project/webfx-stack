package dev.webfx.stack.routing.activity.impl;

import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.ActivityContextFactory;

/**
 * @author Bruno Salmon
 */
public final class ActivityContextFinal extends ActivityContextBase<ActivityContextFinal> {

    public ActivityContextFinal(ActivityContext parentContext, ActivityContextFactory<ActivityContextFinal> contextFactory) {
        super(parentContext, contextFactory);
    }
}
