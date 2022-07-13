package dev.webfx.stack.routing.uirouter.activity.presentation.view;

import dev.webfx.stack.routing.uirouter.activity.presentation.logic.PresentationLogicActivityContext;
import dev.webfx.stack.routing.uirouter.activity.view.ViewActivityContext;

/**
 * @author Bruno Salmon
 */
public interface PresentationViewActivityContext
       <THIS extends PresentationViewActivityContext<THIS, PM>, PM>

       extends PresentationLogicActivityContext<THIS, PM>,
        ViewActivityContext<THIS> {

}
