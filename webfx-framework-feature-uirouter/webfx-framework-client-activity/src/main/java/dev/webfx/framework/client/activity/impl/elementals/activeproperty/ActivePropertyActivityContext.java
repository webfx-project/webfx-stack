package dev.webfx.framework.client.activity.impl.elementals.activeproperty;

import dev.webfx.framework.client.activity.ActivityContext;

/**
 * @author Bruno Salmon
 */
public interface ActivePropertyActivityContext
        <THIS extends ActivePropertyActivityContext<THIS>>

        extends ActivityContext<THIS>,
        HasActiveProperty {

}
