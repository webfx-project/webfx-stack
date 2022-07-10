package dev.webfx.stack.framework.client.activity.impl.elementals.presentation.logic;

import dev.webfx.stack.framework.client.activity.impl.elementals.activeproperty.ActivePropertyActivity;

/**
 * @author Bruno Salmon
 */
public interface PresentationLogicActivity
        <C extends PresentationLogicActivityContext<C, PM>, PM>

        extends ActivePropertyActivity<C> {
}
