package dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl;

import dev.webfx.stack.orm.domainmodel.activity.viewdomain.ViewDomainActivityContextMixin;
import dev.webfx.stack.routing.uirouter.activity.view.impl.ViewActivityBase;

/**
 * @author Bruno Salmon
 */
public abstract class ViewDomainActivityBase extends ViewActivityBase<ViewDomainActivityContextFinal>
    implements ViewDomainActivityContextMixin<ViewDomainActivityContextFinal> {
}
