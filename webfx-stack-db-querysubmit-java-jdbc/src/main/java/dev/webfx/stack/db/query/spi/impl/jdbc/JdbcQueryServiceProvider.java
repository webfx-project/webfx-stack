package dev.webfx.stack.db.query.spi.impl.jdbc;

import dev.webfx.stack.db.querysubmit.jdbc.JdbcLocalConnectedQuerySubmitServiceProvider;
import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.db.query.spi.impl.LocalOrRemoteQueryServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class JdbcQueryServiceProvider extends LocalOrRemoteQueryServiceProvider {

    @Override
    protected QueryServiceProvider createLocalConnectedProvider(LocalDataSource localDataSource) {
        return new JdbcLocalConnectedQuerySubmitServiceProvider(localDataSource);
    }

}
