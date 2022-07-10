package dev.webfx.stack.framework.client.activity.impl.elementals.domain;

import dev.webfx.stack.framework.shared.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.framework.shared.orm.domainmodel.HasDataSourceModel;
import dev.webfx.stack.framework.client.activity.ActivityContextMixin;

/**
 * @author Bruno Salmon
 */
public interface DomainActivityContextMixin
        <C extends DomainActivityContext<C>>

        extends ActivityContextMixin<C>,
        DomainActivityContext<C>,
        HasDataSourceModel {

    @Override
    default C setDataSourceModel(DataSourceModel dataSourceModel) {
        return getActivityContext().setDataSourceModel(dataSourceModel);
    }

    @Override
    default DataSourceModel getDataSourceModel() {
        return getActivityContext().getDataSourceModel();
    }
}
