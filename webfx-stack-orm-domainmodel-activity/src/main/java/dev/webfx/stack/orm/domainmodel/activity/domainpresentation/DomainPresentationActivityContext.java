package dev.webfx.stack.orm.domainmodel.activity.domainpresentation;

import dev.webfx.stack.orm.domainmodel.activity.domain.DomainActivityContext;
import dev.webfx.stack.routing.uirouter.activity.presentation.PresentationActivityContext;
import dev.webfx.stack.routing.uirouter.activity.presentation.logic.PresentationLogicActivityContext;
import dev.webfx.stack.routing.uirouter.activity.presentation.view.PresentationViewActivityContext;

/**
 * @author Bruno Salmon
 */
public interface DomainPresentationActivityContext
        <THIS extends DomainPresentationActivityContext<THIS, C1, C2, PM>,
                C1 extends PresentationViewActivityContext<C1, PM>,
                C2 extends PresentationLogicActivityContext<C2, PM>,
                PM>

        extends DomainActivityContext<THIS>,
        PresentationActivityContext<THIS, C1, C2, PM> {

}
