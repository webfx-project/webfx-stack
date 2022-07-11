package dev.webfx.stack.db.submit.spi.impl.vertx;

import dev.webfx.stack.db.submit.spi.impl.LocalSubmitServiceProvider;
import dev.webfx.stack.db.querysubmit.VertxLocalConnectedQuerySubmitServiceProvider;
import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class VertxSubmitServiceProvider extends LocalSubmitServiceProvider {

    @Override
    protected SubmitServiceProvider createLocalConnectedSubmitService(LocalDataSource localDataSource) {
        return new VertxLocalConnectedQuerySubmitServiceProvider(localDataSource);
    }

}
