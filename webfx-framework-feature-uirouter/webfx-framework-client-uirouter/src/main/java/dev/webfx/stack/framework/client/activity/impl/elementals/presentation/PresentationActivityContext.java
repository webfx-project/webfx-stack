package dev.webfx.stack.framework.client.activity.impl.elementals.presentation;

import dev.webfx.stack.framework.client.activity.impl.elementals.presentation.logic.PresentationLogicActivityContext;
import dev.webfx.stack.framework.client.activity.impl.elementals.view.ViewActivityContext;
import dev.webfx.stack.framework.client.activity.impl.elementals.presentation.view.PresentationViewActivityContext;
import dev.webfx.stack.framework.client.activity.impl.composition.ComposedActivityContext;

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
