package dev.webfx.stack.routing.activity.impl;

import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.ActivityContextFactory;
import dev.webfx.stack.routing.activity.ActivityManager;
import dev.webfx.stack.routing.activity.HasActivityContext;

import java.util.function.Predicate;

/**
 * @author Bruno Salmon
 */
public class ActivityContextBase
        <THIS extends ActivityContextBase<THIS>>

        implements ActivityContext<THIS> {

    private final ActivityContext parentContext;
    private ActivityManager<THIS> activityManager;
    private final ActivityContextFactory<THIS> contextFactory;

    protected ActivityContextBase(ActivityContext parentContext, ActivityContextFactory<THIS> contextFactory) {
        this.parentContext = parentContext;
        this.contextFactory = contextFactory;
    }

    @Override
    public ActivityContext getParentContext() {
        return parentContext;
    }

    public void setActivityManager(ActivityManager<THIS> activityManager) {
        this.activityManager = activityManager;
    }

    @Override
    public ActivityManager<THIS> getActivityManager() {
        return activityManager;
    }

    @Override
    public ActivityContextFactory<THIS> getActivityContextFactory() {
        return contextFactory;
    }

    public static <IC extends ActivityContext<IC>, OC extends ActivityContextBase<OC>> OC toActivityContextBase(IC activityContext) {
        return from(activityContext, ac -> ac instanceof ActivityContextBase);
    }

    public static <IC, OC> OC from(IC activityContext, Predicate<IC> instanceOfPredicate) {
        if (instanceOfPredicate.test(activityContext))
            return (OC) activityContext;
        if (activityContext instanceof HasActivityContext) // including ActivityContextMixin
            return from((IC) ((HasActivityContext) activityContext).getActivityContext(), instanceOfPredicate);
        return null;
    }
}
