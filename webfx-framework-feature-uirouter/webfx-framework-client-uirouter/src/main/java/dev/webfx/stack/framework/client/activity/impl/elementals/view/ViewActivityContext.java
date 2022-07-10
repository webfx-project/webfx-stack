package dev.webfx.stack.framework.client.activity.impl.elementals.view;

import dev.webfx.stack.framework.client.activity.impl.elementals.uiroute.UiRouteActivityContext;
import dev.webfx.stack.framework.client.activity.impl.elementals.view.impl.ViewActivityContextFinal;
import dev.webfx.stack.framework.client.activity.ActivityContext;

/**
 * @author Bruno Salmon
 */
public interface ViewActivityContext
        <THIS extends ViewActivityContext<THIS>>

        extends UiRouteActivityContext<THIS>,
        HasNodeProperty,
        HasMountNodeProperty {

    static ViewActivityContextFinal create(ActivityContext parent) {
        return new ViewActivityContextFinal(parent, ViewActivityContext::create);
    }

}
