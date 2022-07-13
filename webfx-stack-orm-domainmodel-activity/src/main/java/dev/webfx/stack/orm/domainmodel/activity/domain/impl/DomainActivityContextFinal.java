package dev.webfx.stack.orm.domainmodel.activity.domain.impl;

import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.ActivityContextFactory;

/**
 * @author Bruno Salmon
 */
public final class DomainActivityContextFinal extends DomainActivityContextBase<DomainActivityContextFinal> {

    public DomainActivityContextFinal(ActivityContext parentContext, ActivityContextFactory<DomainActivityContextFinal> contextFactory) {
        super(parentContext, contextFactory);
    }

}
