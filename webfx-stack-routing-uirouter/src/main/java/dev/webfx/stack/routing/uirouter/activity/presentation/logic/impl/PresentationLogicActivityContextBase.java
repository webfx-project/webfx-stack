package dev.webfx.stack.routing.uirouter.activity.presentation.logic.impl;

import dev.webfx.stack.routing.uirouter.activity.presentation.logic.PresentationLogicActivityContext;
import dev.webfx.stack.routing.uirouter.activity.uiroute.impl.UiRouteActivityContextBase;
import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.ActivityContextFactory;

/**
 * @author Bruno Salmon
 */
public class PresentationLogicActivityContextBase
        <THIS extends PresentationLogicActivityContextBase<THIS, PM>, PM>

        extends UiRouteActivityContextBase<THIS>
        implements PresentationLogicActivityContext<THIS, PM> {

    public PresentationLogicActivityContextBase(ActivityContext parentContext, ActivityContextFactory<THIS> contextFactory) {
        super(parentContext, contextFactory);
    }

    private PM presentationModel;

    @Override
    public PM getPresentationModel() {
        return presentationModel;
    }

    void setPresentationModel(PM presentationModel) {
        this.presentationModel = presentationModel;
    }

    public static <THIS extends PresentationLogicActivityContextBase<THIS, PM>, PM> THIS of(ActivityContext activityContext) {
        if (activityContext instanceof PresentationLogicActivityContextBase)
            return (THIS) activityContext;
        return null;
    }

}
