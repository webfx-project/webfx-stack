package dev.webfx.stack.framework.client.activity.impl.elementals.presentation.logic;

import dev.webfx.stack.framework.client.activity.impl.elementals.uiroute.UiRouteActivityContext;

/**
 * @author Bruno Salmon
 */
public interface PresentationLogicActivityContext
       <THIS extends PresentationLogicActivityContext<THIS, PM>, PM>

       extends UiRouteActivityContext<THIS> {

    PM getPresentationModel();
}
