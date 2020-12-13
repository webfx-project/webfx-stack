package dev.webfx.framework.client.activity.impl.elementals.domain.impl;

import dev.webfx.framework.client.activity.ActivityContext;
import dev.webfx.framework.client.activity.ActivityContextFactory;

/**
 * @author Bruno Salmon
 */
public final class DomainActivityContextFinal extends DomainActivityContextBase<DomainActivityContextFinal> {

    public DomainActivityContextFinal(ActivityContext parentContext, ActivityContextFactory<DomainActivityContextFinal> contextFactory) {
        super(parentContext, contextFactory);
    }

}
