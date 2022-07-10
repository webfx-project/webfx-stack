package dev.webfx.stack.framework.client.activity.impl.elementals.domain;

import dev.webfx.stack.framework.client.activity.impl.elementals.domain.impl.DomainActivityContextFinal;
import dev.webfx.stack.framework.shared.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.framework.shared.orm.domainmodel.HasDataSourceModel;
import dev.webfx.stack.framework.client.activity.ActivityContext;

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
