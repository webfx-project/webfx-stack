package dev.webfx.stack.orm.domainmodel.activity.domain;

import dev.webfx.stack.orm.domainmodel.activity.domain.impl.DomainActivityContextFinal;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.domainmodel.HasDataSourceModel;
import dev.webfx.stack.routing.activity.ActivityContext;

/**
 * @author Bruno Salmon
 */
public interface DomainActivityContext
        <THIS extends DomainActivityContext<THIS>>

        extends ActivityContext<THIS>,
        HasDataSourceModel {

    THIS setDataSourceModel(DataSourceModel dataSourceModel);

    static DomainActivityContextFinal createDomainActivityContext(ActivityContext parentContext) {
        return new DomainActivityContextFinal(parentContext, DomainActivityContext::createDomainActivityContext);
    }

    static DomainActivityContextFinal createDomainActivityContext(ActivityContext parentContext, DataSourceModel dataSourceModel) {
        return createDomainActivityContext(parentContext).setDataSourceModel(dataSourceModel);
    }

    static DomainActivityContextFinal createDomainActivityContext(DataSourceModel dataSourceModel) {
        return createDomainActivityContext(null, dataSourceModel);
    }

}
