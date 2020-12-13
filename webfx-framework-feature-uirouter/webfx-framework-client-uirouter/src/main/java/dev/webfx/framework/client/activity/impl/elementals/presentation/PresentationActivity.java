package dev.webfx.framework.client.activity.impl.elementals.presentation;

import dev.webfx.framework.client.activity.impl.elementals.presentation.logic.PresentationLogicActivityContext;
import dev.webfx.framework.client.activity.impl.elementals.presentation.view.PresentationViewActivityContext;
import dev.webfx.framework.client.activity.impl.composition.ComposedActivity;

/**
 * @author Bruno Salmon
 */
public interface PresentationActivity
        <C extends PresentationActivityContext<C, C1, C2, PM>,
                C1 extends PresentationViewActivityContext<C1, PM>,
                C2 extends PresentationLogicActivityContext<C2, PM>,
                PM>

    extends ComposedActivity<C, C1, C2> {
}
