package dev.webfx.stack.framework.client.activity.impl.elementals.activeproperty;

import dev.webfx.stack.framework.client.activity.ActivityContext;

/**
 * @author Bruno Salmon
 */
public interface ActivePropertyActivityContext
        <THIS extends ActivePropertyActivityContext<THIS>>

        extends ActivityContext<THIS>,
        HasActiveProperty {

}
