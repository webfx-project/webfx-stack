package dev.webfx.stack.db.query.spi.impl;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.boot.spi.impl.ApplicationModuleBooterManager;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;

/**
 * @author Bruno Salmon
 */
public class LocalQueryServiceProvider implements QueryServiceProvider {

    @Override
    public Future<QueryResult> executeQuery(QueryArgument argument) {
        Object dataSourceId = argument.getDataSourceId();
        String queryString = argument.getStatement();
        // Builds its own line rather than going through QueryArgument.toString(), so it needs the
        // same treatment: a count, never the values. This runs on EVERY query.
        //
        // Gated for a command-line executable, where it is a DUPLICATE: a CLI issuing DQL prints the statement
        // it was given, so this line repeats it one line earlier, and a 22-probe sweep repeated it 44 times.
        // Nothing changes for a browser or a server, where isCLI() is false - on the server this IS the query
        // log. -Dwebfx.boot.log=true brings it back, which is what one wants when the question is what a
        // client actually sent rather than what it meant to send.
        if (ApplicationModuleBooterManager.isProgressLogging())
            Console.log("Query: " + queryString + (argument.getParameters() == null ? "" : "\nParameters: " + QueryArgument.describeParameters(argument.getParameters())));
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
