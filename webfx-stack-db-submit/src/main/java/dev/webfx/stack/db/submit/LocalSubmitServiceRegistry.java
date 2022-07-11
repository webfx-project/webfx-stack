package dev.webfx.stack.db.submit;

import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class LocalSubmitServiceRegistry {

    private static Map</* dataSourceId */ Object, SubmitServiceProvider> localConnectedSubmitServices;

    public static void registerLocalConnectedSubmitService(Object dataSourceId, SubmitServiceProvider localSubmitServiceProvider) {
        if (localConnectedSubmitServices == null)
            localConnectedSubmitServices = new HashMap<>();
        localConnectedSubmitServices.put(dataSourceId, localSubmitServiceProvider);
    }

    public static SubmitServiceProvider getLocalConnectedSubmitService(Object dataSourceId) {
        return localConnectedSubmitServices == null ? null : localConnectedSubmitServices.get(dataSourceId);
    }
}
