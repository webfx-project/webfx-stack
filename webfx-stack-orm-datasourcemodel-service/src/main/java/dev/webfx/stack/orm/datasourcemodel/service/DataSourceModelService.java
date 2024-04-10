package dev.webfx.stack.orm.datasourcemodel.service;

import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.datasourcemodel.service.spi.DataSourceModelProvider;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.service.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public class DataSourceModelService {

    public static DataSourceModelProvider getProvider() {
        return SingleServiceProvider.getProvider(DataSourceModelProvider.class, () -> ServiceLoader.load(DataSourceModelProvider.class));
    }

    public static Object getDefaultDataSourceId() {
        return getProvider().getDefaultDataSourceId();
    }

    public static DataSourceModel getDefaultDataSourceModel() {
        return getProvider().getDefaultDataSourceModel();
    }

    public static Future<DataSourceModel> loadDataSourceModel(Object dataSourceId) {
        return getProvider().loadDataSourceModel(dataSourceId);
    }

    public static DataSourceModel getDataSourceModel(Object dataSourceId) {
        return getProvider().getDataSourceModel(dataSourceId);
    }
}
