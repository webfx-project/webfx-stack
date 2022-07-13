package dev.webfx.stack.routing.uirouter.activity.presentation.view;

import dev.webfx.stack.routing.uirouter.activity.view.ViewActivity;

/**
 * @author Bruno Salmon
 */
public interface PresentationViewActivity
        <C extends PresentationViewActivityContext<C, PM>, PM>

    extends ViewActivity<C> {
}
