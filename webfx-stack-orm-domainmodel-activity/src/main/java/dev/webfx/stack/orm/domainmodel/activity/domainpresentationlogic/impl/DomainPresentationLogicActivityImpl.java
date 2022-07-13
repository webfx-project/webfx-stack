package dev.webfx.stack.orm.domainmodel.activity.domainpresentationlogic.impl;

import dev.webfx.stack.orm.domainmodel.activity.domain.DomainActivityContextMixin;
import dev.webfx.stack.routing.uirouter.activity.presentation.logic.impl.PresentationLogicActivityBase;
import dev.webfx.platform.util.function.Factory;

/**
 * @author Bruno Salmon
 */
public abstract class DomainPresentationLogicActivityImpl<PM>
        extends PresentationLogicActivityBase<DomainPresentationLogicActivityContextFinal<PM>, PM>
        implements DomainActivityContextMixin<DomainPresentationLogicActivityContextFinal<PM>>  {

    public DomainPresentationLogicActivityImpl() {
    }

    public DomainPresentationLogicActivityImpl(Factory<PM> presentationModelFactory) {
        super(presentationModelFactory);
    }
}
