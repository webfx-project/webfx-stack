package dev.webfx.framework.client.activity.impl.combinations.viewdomain;

import dev.webfx.framework.client.activity.impl.elementals.domain.DomainActivityContextMixin;
import dev.webfx.framework.client.activity.impl.elementals.view.ViewActivityContextMixin;

/**
 * @author Bruno Salmon
 */
public interface ViewDomainActivityContextMixin
        <C extends ViewDomainActivityContext<C>>

        extends ViewDomainActivityContext<C>,
        ViewActivityContextMixin<C>,
        DomainActivityContextMixin<C> {
}
