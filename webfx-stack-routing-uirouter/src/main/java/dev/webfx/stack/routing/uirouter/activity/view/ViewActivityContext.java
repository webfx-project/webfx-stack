package dev.webfx.stack.routing.uirouter.activity.view;

import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.uirouter.activity.view.impl.ViewActivityContextFinal;
import dev.webfx.stack.routing.activity.ActivityContext;

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
