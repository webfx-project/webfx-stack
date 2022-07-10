package dev.webfx.stack.framework.client.activity.impl.combinations.viewdomain;

import dev.webfx.stack.framework.client.activity.ActivityContext;
import dev.webfx.stack.framework.client.activity.impl.combinations.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.framework.client.activity.impl.elementals.domain.DomainActivityContext;
import dev.webfx.stack.framework.client.activity.impl.elementals.view.ViewActivityContext;
import dev.webfx.stack.framework.shared.orm.domainmodel.DataSourceModel;

/**
 * @author Bruno Salmon
 */
public interface ViewDomainActivityContext
        <THIS extends ViewDomainActivityContext<THIS>>

        extends ViewActivityContext<THIS>,
        DomainActivityContext<THIS> {

    static ViewDomainActivityContextFinal create(ActivityContext parentContext) {
        return new ViewDomainActivityContextFinal(parentContext);
    }

    static ViewDomainActivityContextFinal createViewDomainActivityContext(DataSourceModel dataSourceModel) {
        return create(null).setDataSourceModel(dataSourceModel);
    }

}
