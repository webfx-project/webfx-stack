package dev.webfx.stack.routing.activity.impl.composition.impl;

import dev.webfx.platform.util.function.Callable;
import dev.webfx.platform.util.function.Factory;
import dev.webfx.platform.async.util.AsyncUtil;
import dev.webfx.platform.async.Future;
import dev.webfx.stack.routing.activity.Activity;
import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.ActivityContextFactory;
import dev.webfx.stack.routing.activity.ActivityManager;
import dev.webfx.stack.routing.activity.impl.ActivityBase;
import dev.webfx.stack.routing.activity.impl.composition.ComposedActivity;
import dev.webfx.stack.routing.activity.impl.composition.ComposedActivityContext;
import dev.webfx.stack.routing.activity.impl.composition.ComposedActivityContextMixin;

/**
 * @author Bruno Salmon
 */
public class ComposedActivityBase
        <C extends ComposedActivityContext<C, C1, C2>,
                C1 extends ActivityContext<C1>,
                C2 extends ActivityContext<C2>>

    extends ActivityBase<C>
    implements ComposedActivity<C, C1, C2>,
        ComposedActivityContextMixin<C, C1, C2> {

    private final Factory<ActivityManager<C1>> activityManagerFactory1;
    private final Factory<ActivityManager<C2>> activityManagerFactory2;

    public ComposedActivityBase(Factory<Activity<C1>> activityFactory1, ActivityContextFactory<C1> contextFactory1, Factory<Activity<C2>> activityFactory2, ActivityContextFactory<C2> contextFactory2) {
        this(ActivityManager.factory(activityFactory1, contextFactory1), ActivityManager.factory(activityFactory2, contextFactory2));
    }

    public ComposedActivityBase(Factory<ActivityManager<C1>> activityManagerFactory1, Factory<ActivityManager<C2>> activityManagerFactory2) {
        this.activityManagerFactory1 = activityManagerFactory1;
        this.activityManagerFactory2 = activityManagerFactory2;
    }

    @Override
    public Future<Void> onCreateAsync(C context) {
        onCreate(context);
        ActivityManager<C1> activityManager1 = activityManagerFactory1.create();
        ActivityManager<C2> activityManager2 = activityManagerFactory2.create();
        C1 context1 = activityManager1.getContextFactory().createContext(context);
        C2 context2 = activityManager2.getContextFactory().createContext(context);
        context.setActivityContext1(context1);
        context.setActivityContext2(context2);
        return AsyncUtil.allOf(activityManager1.create(context1), activityManager2.create(context2));
    }

    @Override
    public Future<Void> onStartAsync() {
        return executeBoth(getActivityManager1()::start, getActivityManager2()::start);
    }

    @Override
    public Future<Void> onResumeAsync() {
        return executeBoth(getActivityManager1()::resume, getActivityManager2()::resume);
    }

    @Override
    public Future<Void> onPauseAsync() {
        return executeBoth(getActivityManager1()::pause, getActivityManager2()::pause);
    }

    @Override
    public Future<Void> onStopAsync() {
        return executeBoth(getActivityManager1()::stop, getActivityManager2()::stop);
    }

    @Override
    public Future<Void> onDestroyAsync() {
        return executeBoth(getActivityManager1()::destroy, getActivityManager2()::destroy);
    }

    protected Future<Void> executeBoth(Callable<Future<Void>> callable1, Callable<Future<Void>> callable2) {
        return AsyncUtil.allOf(callable1.call(), callable2.call());
    }
}
