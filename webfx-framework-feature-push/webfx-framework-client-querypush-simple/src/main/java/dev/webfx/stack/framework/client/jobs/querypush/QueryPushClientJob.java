package dev.webfx.stack.framework.client.jobs.querypush;

import dev.webfx.stack.framework.client.services.querypush.QueryPushClientService;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.framework.shared.services.querypush.spi.impl.LocalOrRemoteQueryPushServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class QueryPushClientJob implements ApplicationJob {

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
