package dev.webfx.framework.client.jobs.querypush;

import dev.webfx.framework.client.services.querypush.QueryPushClientService;
import dev.webfx.platform.shared.services.appcontainer.spi.ApplicationJob;
import dev.webfx.platform.shared.services.bus.Registration;
import dev.webfx.framework.shared.services.querypush.spi.impl.LocalOrRemoteQueryPushServiceProvider;

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
