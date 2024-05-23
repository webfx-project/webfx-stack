package dev.webfx.stack.db.submit.spi.impl.vertx;

import dev.webfx.stack.db.datasource.DBMS;
import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.querysubmit.VertxLocalPostgresQuerySubmitServiceProvider;
import dev.webfx.stack.db.querysubmit.VertxLocalQuerySubmitServiceProvider;
import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;
import dev.webfx.stack.db.submit.spi.impl.LocalSubmitServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class VertxLocalSubmitServiceProvider extends LocalSubmitServiceProvider {

    @Override
    protected SubmitServiceProvider createLocalConnectedSubmitService(LocalDataSource localDataSource) {
        if (localDataSource.getDBMS() == DBMS.POSTGRES)
            return new RetryOnSocketExceptionSubmitServiceProvider(
                    new VertxLocalPostgresQuerySubmitServiceProvider(localDataSource)
            );
        return new VertxLocalQuerySubmitServiceProvider(localDataSource);
    }

}
