package dev.webfx.stack.db.submit.spi.impl.jdbc;

import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.querysubmit.jdbc.JdbcLocalConnectedQuerySubmitServiceProvider;
import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;
import dev.webfx.stack.db.submit.spi.impl.LocalSubmitServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class JdbcSubmitServiceProvider extends LocalSubmitServiceProvider { /* was LocalOrRemoteSubmitServiceProvider but used only by kbs2-to-modality-modelimport so Local only (no need for BusCall dependencies) */

    @Override
    protected SubmitServiceProvider createLocalConnectedSubmitService(LocalDataSource localDataSource) {
        return new JdbcLocalConnectedQuerySubmitServiceProvider(localDataSource);
    }

}
