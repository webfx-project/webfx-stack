package dev.webfx.stack.framework.client.activity.impl.elementals.presentation;

import dev.webfx.stack.framework.client.activity.impl.elementals.presentation.logic.PresentationLogicActivityContext;
import dev.webfx.stack.framework.client.activity.impl.elementals.view.ViewActivityContextMixin;
import dev.webfx.stack.framework.client.activity.impl.elementals.presentation.view.PresentationViewActivityContext;
import dev.webfx.stack.framework.client.activity.impl.composition.ComposedActivityContextMixin;

/**
 * @author Bruno Salmon
 */
public interface PresentationActivityContextMixin
        <C extends PresentationActivityContext<C, C1, C2, PM>,
                C1 extends PresentationViewActivityContext<C1, PM>,
                C2 extends PresentationLogicActivityContext<C2, PM>,
                PM>

       extends ComposedActivityContextMixin<C, C1, C2>,
        ViewActivityContextMixin<C>,
        PresentationActivityContext<C, C1, C2, PM> {

}
