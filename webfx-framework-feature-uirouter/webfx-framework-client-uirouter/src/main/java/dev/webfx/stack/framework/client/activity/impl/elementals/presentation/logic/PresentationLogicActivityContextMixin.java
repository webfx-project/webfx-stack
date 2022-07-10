package dev.webfx.stack.framework.client.activity.impl.elementals.presentation.logic;

import dev.webfx.stack.framework.client.activity.impl.elementals.uiroute.UiRouteActivityContextMixin;

/**
 * @author Bruno Salmon
 */
public interface PresentationLogicActivityContextMixin
       <C extends PresentationLogicActivityContext<C, PM>, PM>

       extends UiRouteActivityContextMixin<C>,
        PresentationLogicActivityContext<C, PM> {

    default PM getPresentationModel() {
        return getActivityContext().getPresentationModel();
    }
}
