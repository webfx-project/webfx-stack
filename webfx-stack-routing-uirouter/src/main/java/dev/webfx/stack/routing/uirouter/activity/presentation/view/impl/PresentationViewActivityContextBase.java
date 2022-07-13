package dev.webfx.stack.routing.uirouter.activity.presentation.view.impl;

import dev.webfx.stack.routing.uirouter.activity.view.impl.ViewActivityContextBase;
import dev.webfx.stack.routing.uirouter.activity.presentation.view.PresentationViewActivityContext;
import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.ActivityContextFactory;
import dev.webfx.stack.routing.activity.impl.ActivityContextBase;

/**
 * @author Bruno Salmon
 */
public class PresentationViewActivityContextBase
        <THIS extends PresentationViewActivityContextBase<THIS, PM>, PM>

        extends ViewActivityContextBase<THIS>
        implements PresentationViewActivityContext<THIS, PM> {

    private PM presentationModel;

    public PresentationViewActivityContextBase(ActivityContext parentContext, ActivityContextFactory<THIS> contextFactory) {
        super(parentContext, contextFactory);
    }

    @Override
    public PM getPresentationModel() {
        return presentationModel;
    }

    void setPresentationModel(PM presentationModel) {
        this.presentationModel = presentationModel;
    }

    public static <IC extends ActivityContext<IC>, OC extends PresentationViewActivityContextBase<OC, PM>, PM> OC toViewModelActivityContextBase(IC activityContext) {
        return ActivityContextBase.from(activityContext, ac -> ac instanceof PresentationViewActivityContextBase);
    }

}
