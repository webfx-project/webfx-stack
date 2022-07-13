package dev.webfx.stack.routing.activity.impl.elementals.activeproperty;

import dev.webfx.stack.routing.activity.ActivityContext;

/**
 * @author Bruno Salmon
 */
public interface ActivePropertyActivityContext
        <THIS extends ActivePropertyActivityContext<THIS>>

        extends ActivityContext<THIS>,
        HasActiveProperty {

}
