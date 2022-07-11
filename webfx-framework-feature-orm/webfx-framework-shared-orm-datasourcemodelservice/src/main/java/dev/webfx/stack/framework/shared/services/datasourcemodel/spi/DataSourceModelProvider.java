package dev.webfx.stack.framework.shared.services.datasourcemodel.spi;

import dev.webfx.stack.framework.shared.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.async.Future;

/**
 * @author Bruno Salmon
 */
public interface DataSourceModelProvider {

    Object getDefaultDataSourceId();

    default DataSourceModel getDefaultDataSourceModel() {
        return getDataSourceModel(getDefaultDataSourceId());
    }

    DataSourceModel getDataSourceModel(Object dataSourceId);

    Future<DataSourceModel> loadDataSourceModel(Object dataSourceId);

}
