package dev.webfx.stack.db.datasource;

import dev.webfx.stack.db.datasource.spi.LocalDataSourceProvider;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class LocalDataSourceService {

    private final static LocalDataSourceProvider provider = SingleServiceProvider.getProvider(LocalDataSourceProvider.class, () -> ServiceLoader.load(LocalDataSourceProvider.class), SingleServiceProvider.NotFoundPolicy.RETURN_NULL);

    public static LocalDataSource getLocalDataSource(Object dataSourceId) {
        return provider == null ? null : provider.getLocalDataSource(dataSourceId);
    }

    public static boolean isDataSourceLocal(Object dataSourceId) {
        return provider != null && provider.isDataSourceLocal(dataSourceId);
    }

}
