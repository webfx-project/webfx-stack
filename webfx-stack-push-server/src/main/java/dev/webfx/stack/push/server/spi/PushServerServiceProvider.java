package dev.webfx.stack.push.server.spi;

import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.push.server.UnresponsivePushClientListener;
import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.push.ClientPushBusAddressesSharedByBothClientAndServer;

/**
 * @author Bruno Salmon
 */
public interface PushServerServiceProvider {

    <T> Future<T> push(String clientServiceAddress, Object javaArgument, DeliveryOptions options, Bus bus, Object clientRunId);

    default Future<Void> pushPing(DeliveryOptions options, Bus bus, Object clientRunId) {
        return push(ClientPushBusAddressesSharedByBothClientAndServer.PUSH_PING_CLIENT_LISTENER_SERVICE_ADDRESS, "Push ping to client " + clientRunId, options, bus, clientRunId);
    }

    void clientIsLive(Object clientRunId);

    void addUnresponsivePushClientListener(UnresponsivePushClientListener listener);

    void removeUnresponsivePushClientListener(UnresponsivePushClientListener listener);

}
