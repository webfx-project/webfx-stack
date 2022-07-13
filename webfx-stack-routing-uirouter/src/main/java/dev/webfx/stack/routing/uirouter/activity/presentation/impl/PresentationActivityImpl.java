package dev.webfx.stack.routing.uirouter.activity.presentation.impl;

import dev.webfx.platform.util.function.Factory;
import dev.webfx.stack.routing.uirouter.activity.presentation.logic.impl.PresentationLogicActivityContextFinal;
import dev.webfx.stack.routing.uirouter.activity.presentation.view.impl.PresentationViewActivityContextFinal;
import dev.webfx.stack.routing.activity.Activity;

/**
 * @author Bruno Salmon
 */
public abstract class PresentationActivityImpl<PM>
        extends PresentationActivityBase<PresentationActivityContextFinal<PM>, PresentationViewActivityContextFinal<PM>, PresentationLogicActivityContextFinal<PM>, PM> {

    public PresentationActivityImpl(Factory<Activity<PresentationViewActivityContextFinal<PM>>> activityFactory1, Factory<Activity<PresentationLogicActivityContextFinal<PM>>> activityFactory2) {
        super(activityFactory1, PresentationViewActivityContextFinal::new, activityFactory2, PresentationLogicActivityContextFinal::new);
    }

}
