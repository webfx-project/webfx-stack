package dev.webfx.framework.client.activity.impl.combinations.domainpresentation;

import dev.webfx.framework.client.activity.impl.elementals.domain.DomainActivityContext;
import dev.webfx.framework.client.activity.impl.elementals.presentation.PresentationActivityContext;
import dev.webfx.framework.client.activity.impl.elementals.presentation.logic.PresentationLogicActivityContext;
import dev.webfx.framework.client.activity.impl.elementals.presentation.view.PresentationViewActivityContext;

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
