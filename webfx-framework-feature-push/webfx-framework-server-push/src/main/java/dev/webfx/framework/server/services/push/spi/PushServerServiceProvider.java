package dev.webfx.framework.server.services.push.spi;

import dev.webfx.framework.server.services.push.UnresponsivePushClientListener;
import dev.webfx.platform.shared.async.Future;
import dev.webfx.platform.shared.services.bus.Bus;
import dev.webfx.framework.shared.services.push.ClientPushBusAddressesSharedByBothClientAndServer;

/**
 * @author Bruno Salmon
 */
public interface PushServerServiceProvider {

    <T> Future<T> callClientService(String serviceAddress, Object javaArgument, Bus bus, Object pushClientId);

    default Future pingPushClient(Bus bus, Object pushClientId) {
        return callClientService(ClientPushBusAddressesSharedByBothClientAndServer.PUSH_PING_CLIENT_LISTENER_SERVICE_ADDRESS, "Server ping for push client " + pushClientId, bus, pushClientId);
    }

    void addUnresponsivePushClientListener(UnresponsivePushClientListener listener);

    void removeUnresponsivePushClientListener(UnresponsivePushClientListener listener);

}
