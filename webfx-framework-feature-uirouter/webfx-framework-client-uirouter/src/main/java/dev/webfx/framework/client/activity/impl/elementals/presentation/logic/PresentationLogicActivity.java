package dev.webfx.framework.client.activity.impl.elementals.presentation.logic;

import dev.webfx.framework.client.activity.impl.elementals.activeproperty.ActivePropertyActivity;

/**
 * @author Bruno Salmon
 */
public interface PresentationLogicActivity
        <C extends PresentationLogicActivityContext<C, PM>, PM>

        extends ActivePropertyActivity<C> {
}
