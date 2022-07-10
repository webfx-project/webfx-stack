package dev.webfx.stack.framework.client.activity.impl;

import dev.webfx.stack.framework.client.activity.ActivityContext;
import dev.webfx.stack.framework.client.activity.ActivityContextFactory;

/**
 * @author Bruno Salmon
 */
public final class ActivityContextFinal extends ActivityContextBase<ActivityContextFinal> {

    public ActivityContextFinal(ActivityContext parentContext, ActivityContextFactory<ActivityContextFinal> contextFactory) {
        super(parentContext, contextFactory);
    }
}
