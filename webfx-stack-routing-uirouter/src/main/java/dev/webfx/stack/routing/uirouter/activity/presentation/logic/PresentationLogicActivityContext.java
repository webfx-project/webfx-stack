package dev.webfx.stack.routing.uirouter.activity.presentation.logic;

import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;

/**
 * @author Bruno Salmon
 */
public interface PresentationLogicActivityContext
       <THIS extends PresentationLogicActivityContext<THIS, PM>, PM>

       extends UiRouteActivityContext<THIS> {

    PM getPresentationModel();
}
