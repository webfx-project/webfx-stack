package dev.webfx.stack.orm.domainmodel.activity.viewdomain;

import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.orm.domainmodel.activity.domain.DomainActivityContext;
import dev.webfx.stack.routing.uirouter.activity.view.ViewActivityContext;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;

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
