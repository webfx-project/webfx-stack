package dev.webfx.stack.orm.datasourcemodel.service.spi;

import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.platform.async.Future;

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
