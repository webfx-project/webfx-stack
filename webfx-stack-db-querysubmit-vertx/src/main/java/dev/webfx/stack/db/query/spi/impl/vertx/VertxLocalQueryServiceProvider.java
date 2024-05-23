package dev.webfx.stack.db.query.spi.impl.vertx;

import dev.webfx.stack.db.datasource.DBMS;
import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.db.query.spi.impl.LocalQueryServiceProvider;
import dev.webfx.stack.db.querysubmit.VertxLocalPostgresQuerySubmitServiceProvider;
import dev.webfx.stack.db.querysubmit.VertxLocalQuerySubmitServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class VertxLocalQueryServiceProvider extends LocalQueryServiceProvider {

    @Override
    protected QueryServiceProvider createLocalConnectedProvider(LocalDataSource localDataSource) {
        if (localDataSource.getDBMS() == DBMS.POSTGRES)
            return new VertxLocalPostgresQuerySubmitServiceProvider(localDataSource);
        return new VertxLocalQuerySubmitServiceProvider(localDataSource);
    }

}
