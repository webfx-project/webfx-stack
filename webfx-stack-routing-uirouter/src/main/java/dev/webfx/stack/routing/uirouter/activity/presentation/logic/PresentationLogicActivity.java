package dev.webfx.stack.routing.uirouter.activity.presentation.logic;

import dev.webfx.stack.routing.activity.impl.elementals.activeproperty.ActivePropertyActivity;

/**
 * @author Bruno Salmon
 */
public interface PresentationLogicActivity
        <C extends PresentationLogicActivityContext<C, PM>, PM>

        extends ActivePropertyActivity<C> {
}
