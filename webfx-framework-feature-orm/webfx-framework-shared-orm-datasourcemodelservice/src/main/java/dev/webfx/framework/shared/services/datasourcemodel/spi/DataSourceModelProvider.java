package dev.webfx.framework.shared.services.datasourcemodel.spi;

import dev.webfx.framework.shared.orm.domainmodel.DataSourceModel;
import dev.webfx.platform.shared.async.Future;

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
