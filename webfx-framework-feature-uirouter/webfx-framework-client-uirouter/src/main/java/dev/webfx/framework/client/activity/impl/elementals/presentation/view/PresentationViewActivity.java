package dev.webfx.framework.client.activity.impl.elementals.presentation.view;

import dev.webfx.framework.client.activity.impl.elementals.view.ViewActivity;

/**
 * @author Bruno Salmon
 */
public interface PresentationViewActivity
        <C extends PresentationViewActivityContext<C, PM>, PM>

    extends ViewActivity<C> {
}
