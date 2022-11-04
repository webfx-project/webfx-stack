package dev.webfx.stack.querypush.client.simple;

import dev.webfx.stack.querypush.client.QueryPushClientService;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.querypush.spi.impl.remote.LocalOrRemoteQueryPushServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class SimpleQueryPushClientJob implements ApplicationJob {

    private Registration registration;

    @Override
    public void onStart() {
        registration = QueryPushClientService.registerQueryPushClientConsumer(LocalOrRemoteQueryPushServiceProvider::onQueryPushResultReceived);
    }

    @Override
    public void onStop() {
        if (registration != null)
            registration.unregister();
    }
}
