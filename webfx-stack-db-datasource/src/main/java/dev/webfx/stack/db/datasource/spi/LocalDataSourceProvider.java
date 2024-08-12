package dev.webfx.stack.db.datasource.spi;

import dev.webfx.stack.db.datasource.LocalDataSource;

/**
 * @author Bruno Salmon
 */
public interface LocalDataSourceProvider {

    LocalDataSource getLocalDataSource(Object dataSourceId);

    default boolean isDataSourceLocal(Object dataSourceId) {
        return getLocalDataSource(dataSourceId) != null;
    }

    boolean isInitialised();

    void onInitialised(Runnable runnable);

}
