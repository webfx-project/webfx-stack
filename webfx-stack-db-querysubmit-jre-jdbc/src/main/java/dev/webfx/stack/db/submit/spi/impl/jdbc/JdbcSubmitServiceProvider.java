package dev.webfx.stack.db.submit.spi.impl.jdbc;

import dev.webfx.stack.db.querysubmit.jdbc.JdbcLocalConnectedQuerySubmitServiceProvider;
import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;
import dev.webfx.stack.db.submit.spi.impl.remote.LocalOrRemoteSubmitServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class JdbcSubmitServiceProvider extends LocalOrRemoteSubmitServiceProvider {

    @Override
    protected SubmitServiceProvider createLocalConnectedSubmitService(LocalDataSource localDataSource) {
        return new JdbcLocalConnectedQuerySubmitServiceProvider(localDataSource);
    }

}
