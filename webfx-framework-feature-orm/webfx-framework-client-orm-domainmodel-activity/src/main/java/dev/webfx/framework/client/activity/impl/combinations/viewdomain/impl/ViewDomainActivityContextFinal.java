package dev.webfx.framework.client.activity.impl.combinations.viewdomain.impl;

import dev.webfx.framework.client.activity.ActivityContext;

/**
 * @author Bruno Salmon
 */
public final class ViewDomainActivityContextFinal extends ViewDomainActivityContextBase<ViewDomainActivityContextFinal> {

    public ViewDomainActivityContextFinal(ActivityContext parentContext) {
        super(parentContext, ViewDomainActivityContextFinal::new);
    }
}
