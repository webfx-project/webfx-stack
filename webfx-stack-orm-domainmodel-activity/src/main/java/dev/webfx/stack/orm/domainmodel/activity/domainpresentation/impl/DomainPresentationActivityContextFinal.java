package dev.webfx.stack.orm.domainmodel.activity.domainpresentation.impl;

import dev.webfx.stack.orm.domainmodel.activity.domainpresentationlogic.impl.DomainPresentationLogicActivityContextFinal;
import dev.webfx.stack.routing.uirouter.activity.presentation.view.impl.PresentationViewActivityContextFinal;
import dev.webfx.stack.routing.activity.ActivityContext;

/**
 * @author Bruno Salmon
 */
public final class DomainPresentationActivityContextFinal<PM>
        extends DomainPresentationActivityContextBase<DomainPresentationActivityContextFinal<PM>, PresentationViewActivityContextFinal<PM>, DomainPresentationLogicActivityContextFinal<PM>, PM> {

    public DomainPresentationActivityContextFinal(ActivityContext parentContext) {
        super(parentContext, DomainPresentationActivityContextFinal::new);
    }
}
