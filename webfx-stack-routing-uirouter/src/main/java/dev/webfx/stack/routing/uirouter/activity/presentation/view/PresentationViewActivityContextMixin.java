package dev.webfx.stack.routing.uirouter.activity.presentation.view;

import dev.webfx.stack.routing.activity.ActivityContextMixin;

/**
 * @author Bruno Salmon
 */
public interface PresentationViewActivityContextMixin
       <C extends PresentationViewActivityContext<C, PM>, PM>

       extends ActivityContextMixin<C>,
        PresentationViewActivityContext<C, PM> {

    default PM getPresentationModel() {
        return getActivityContext().getPresentationModel();
    }

}
