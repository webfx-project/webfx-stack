package dev.webfx.stack.db.query.spi.impl.vertx;

import dev.webfx.stack.db.querysubmit.VertxLocalConnectedQuerySubmitServiceProvider;
import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.db.query.spi.impl.LocalQueryServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class VertxQueryServiceProvider extends LocalQueryServiceProvider {

    @Override
    protected QueryServiceProvider createLocalConnectedProvider(LocalDataSource localDataSource) {
        return new VertxLocalConnectedQuerySubmitServiceProvider(localDataSource);
    }

}
