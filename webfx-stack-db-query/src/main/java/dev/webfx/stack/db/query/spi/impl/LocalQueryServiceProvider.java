package dev.webfx.stack.db.query.spi.impl;

import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.async.Future;
import dev.webfx.platform.console.Console;

/**
 * @author Bruno Salmon
 */
public class LocalQueryServiceProvider implements QueryServiceProvider {

    @Override
    public Future<QueryResult> executeQuery(QueryArgument argument) {
        Object dataSourceId = argument.getDataSourceId();
        String queryString = argument.getStatement();
        Console.log("Query: " + queryString + (argument.getParameters() == null ? "" : "\nParameters: " + Arrays.toString(argument.getParameters())));
        QueryServiceProvider localConnectedProvider = getOrCreateLocalConnectedProvider(dataSourceId);
        if (localConnectedProvider != null)
            return localConnectedProvider.executeQuery(argument);
        return executeRemoteQuery(argument);
    }

    protected QueryServiceProvider getOrCreateLocalConnectedProvider(Object dataSourceId) {
        QueryServiceProvider localConnectedProvider = LocalConnectedQueryServiceProviderRegistry.getLocalConnectedProvider(dataSourceId);
        if (localConnectedProvider == null) {
            LocalDataSource localDataSource = LocalDataSource.get(dataSourceId);
            if (localDataSource != null) {
                localConnectedProvider = createLocalConnectedProvider(localDataSource);
                LocalConnectedQueryServiceProviderRegistry.registerLocalConnectedProvider(dataSourceId, localConnectedProvider);
            }
        }
        return localConnectedProvider;
    }

    protected QueryServiceProvider createLocalConnectedProvider(LocalDataSource localDataSource) {
        throw new UnsupportedOperationException("This platform doesn't support local query service");
    }

    protected <T> Future<T> executeRemoteQuery(QueryArgument argument) {
        throw new UnsupportedOperationException("This platform doesn't support remote query service");
    }
}
