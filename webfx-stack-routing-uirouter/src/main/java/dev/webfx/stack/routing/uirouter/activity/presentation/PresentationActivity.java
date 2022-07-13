package dev.webfx.stack.routing.uirouter.activity.presentation;

import dev.webfx.stack.routing.uirouter.activity.presentation.logic.PresentationLogicActivityContext;
import dev.webfx.stack.routing.uirouter.activity.presentation.view.PresentationViewActivityContext;
import dev.webfx.stack.routing.activity.impl.composition.ComposedActivity;

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
