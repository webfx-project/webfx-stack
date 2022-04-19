package dev.webfx.framework.client.activity.impl.elementals.presentation.impl;

import dev.webfx.framework.client.activity.Activity;
import dev.webfx.framework.client.activity.ActivityContextFactory;
import dev.webfx.framework.client.activity.ActivityManager;
import dev.webfx.framework.client.activity.impl.composition.impl.ComposedActivityBase;
import dev.webfx.framework.client.activity.impl.elementals.presentation.PresentationActivity;
import dev.webfx.framework.client.activity.impl.elementals.presentation.PresentationActivityContext;
import dev.webfx.framework.client.activity.impl.elementals.presentation.PresentationActivityContextMixin;
import dev.webfx.framework.client.activity.impl.elementals.presentation.logic.PresentationLogicActivityContext;
import dev.webfx.framework.client.activity.impl.elementals.presentation.view.PresentationViewActivityContext;
import dev.webfx.framework.client.activity.impl.elementals.presentation.view.impl.PresentationViewActivityBase;
import dev.webfx.platform.client.services.uischeduler.UiScheduler;
import dev.webfx.platform.shared.async.AsyncUtil;
import dev.webfx.platform.shared.async.Future;
import dev.webfx.platform.shared.async.Promise;
import dev.webfx.platform.shared.util.function.Callable;
import dev.webfx.platform.shared.util.function.Factory;

/**
 * @author Bruno Salmon
 */
public class PresentationActivityBase
        <C extends PresentationActivityContext<C, C1, C2, PM>,
                C1 extends PresentationViewActivityContext<C1, PM>,
                C2 extends PresentationLogicActivityContext<C2, PM>,
                PM>

        extends ComposedActivityBase<C, C1, C2>
        implements PresentationActivity<C, C1, C2, PM>,
        PresentationActivityContextMixin<C, C1, C2, PM> {

    public PresentationActivityBase(Factory<Activity<C1>> activityFactory1, ActivityContextFactory<C1> contextFactory1, Factory<Activity<C2>> activityFactory2, ActivityContextFactory<C2> contextFactory2) {
        super(activityFactory1, contextFactory1, activityFactory2, contextFactory2);
    }

    public PresentationActivityBase(Factory<ActivityManager<C1>> activityManagerFactory1, Factory<ActivityManager<C2>> activityManagerFactory2) {
        super(activityManagerFactory1, activityManagerFactory2);
    }

    @Override
    public Future<Void> onCreateAsync(C context) {
        Future<Void> future = super.onCreateAsync(context);
        // Ugly parameter passing
        ((PresentationViewActivityBase) getActivityManager1().getActivity()).setPresentationModel(getActivityContext2().getPresentationModel());
        nodeProperty().bind(getActivityContext1().nodeProperty());
        mountNodeProperty().bind(getActivityContext1().mountNodeProperty());
        return future;
    }

    @Override
    protected Future<Void> executeBoth(Callable<Future<Void>> callable1, Callable<Future<Void>> callable2) {
        Promise<Void> promise2 = Promise.promise();
        UiScheduler.runOutUiThread(() -> callable2.call().onComplete(promise2));
        Promise<Void> promise1 = Promise.promise();
        UiScheduler.runInUiThread(() -> callable1.call().onComplete(promise1));
        return AsyncUtil.allOf(promise1.future(), promise2.future());
    }
}
