package dev.webfx.stack.orm.domainmodel.activity.domain;

import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.domainmodel.HasDataSourceModel;
import dev.webfx.stack.routing.activity.ActivityContextMixin;

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
