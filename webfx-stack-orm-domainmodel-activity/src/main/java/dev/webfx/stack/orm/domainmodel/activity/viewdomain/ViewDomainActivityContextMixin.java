package dev.webfx.stack.orm.domainmodel.activity.viewdomain;

import dev.webfx.stack.orm.domainmodel.activity.domain.DomainActivityContextMixin;
import dev.webfx.stack.routing.uirouter.activity.view.ViewActivityContextMixin;

/**
 * @author Bruno Salmon
 */
public interface ViewDomainActivityContextMixin
        <C extends ViewDomainActivityContext<C>>

        extends ViewDomainActivityContext<C>,
        ViewActivityContextMixin<C>,
        DomainActivityContextMixin<C> {
}
