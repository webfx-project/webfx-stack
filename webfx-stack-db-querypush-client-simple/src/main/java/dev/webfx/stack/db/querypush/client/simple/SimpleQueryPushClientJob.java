package dev.webfx.stack.db.querypush.client.simple;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.stack.db.querypush.client.QueryPushClientService;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.db.querypush.spi.impl.remote.LocalOrRemoteQueryPushServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class SimpleQueryPushClientJob implements ApplicationModuleBooter {

    private Registration registration;

    @Override
    public String getModuleName() {
        return "webfx-stack-db-querypush-client-simple";
    }

    @Override
    public int getBootLevel() {
        return COMMUNICATION_REGISTER_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        registration = QueryPushClientService.registerQueryPushClientConsumer(LocalOrRemoteQueryPushServiceProvider::onQueryPushResultReceived);
    }

    @Override
    public void exitModule() {
        if (registration != null)
            registration.unregister();
    }
}
