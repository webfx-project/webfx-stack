package dev.webfx.stack.routing.uirouter.activity.presentation;

import dev.webfx.stack.routing.uirouter.activity.presentation.logic.PresentationLogicActivityContext;
import dev.webfx.stack.routing.uirouter.activity.view.ViewActivityContext;
import dev.webfx.stack.routing.uirouter.activity.presentation.view.PresentationViewActivityContext;
import dev.webfx.stack.routing.activity.impl.composition.ComposedActivityContext;

/**
 * @author Bruno Salmon
 */
public interface PresentationActivityContext
        <THIS extends PresentationActivityContext<THIS, C1, C2, PM>,
                C1 extends PresentationViewActivityContext<C1, PM>,
                C2 extends PresentationLogicActivityContext<C2, PM>,
                PM>

       extends ComposedActivityContext<THIS, C1, C2>,
        ViewActivityContext<THIS> {


}
