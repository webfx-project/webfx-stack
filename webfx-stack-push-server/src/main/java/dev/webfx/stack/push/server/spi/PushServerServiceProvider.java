package dev.webfx.stack.push.server.spi;

import dev.webfx.stack.push.server.UnresponsivePushClientListener;
import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.push.ClientPushBusAddressesSharedByBothClientAndServer;

/**
 * @author Bruno Salmon
 */
public interface PushServerServiceProvider {

    <T> Future<T> push(String clientServiceAddress, Object javaArgument, Object state, Bus bus, Object clientRunId);

    default Future<Void> pushPing(Object state, Bus bus, Object clientRunId) {
        return push(ClientPushBusAddressesSharedByBothClientAndServer.PUSH_PING_CLIENT_LISTENER_SERVICE_ADDRESS, "Push ping to client " + clientRunId, state, bus, clientRunId);
    }

    void addUnresponsivePushClientListener(UnresponsivePushClientListener listener);

    void removeUnresponsivePushClientListener(UnresponsivePushClientListener listener);

}
