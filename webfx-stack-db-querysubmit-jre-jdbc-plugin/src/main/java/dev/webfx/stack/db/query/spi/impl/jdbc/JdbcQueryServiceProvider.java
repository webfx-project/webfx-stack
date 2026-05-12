package dev.webfx.stack.db.query.spi.impl.jdbc;

import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.db.query.spi.impl.LocalQueryServiceProvider;
import dev.webfx.stack.db.querysubmit.jdbc.JdbcLocalConnectedQuerySubmitServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class JdbcQueryServiceProvider extends LocalQueryServiceProvider { /* was LocalOrRemoteQueryServiceProvider but used only by kbs2-to-modality-modelimport so Local only (no need for BusCall dependencies) */

    @Override
    protected QueryServiceProvider createLocalConnectedProvider(LocalDataSource localDataSource) {
        return new JdbcLocalConnectedQuerySubmitServiceProvider(localDataSource);
    }

}
