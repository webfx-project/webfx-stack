package dev.webfx.stack.push.server.spi;

import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.push.server.PushClientMetadata;
import dev.webfx.stack.push.server.UnresponsivePushClientListener;
import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.push.ClientPushBusAddressesSharedByBothClientAndServer;

import java.util.Collections;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public interface PushServerServiceProvider {

    <T> Future<T> push(String clientServiceAddress, Object javaArgument, DeliveryOptions options, Bus bus, Object clientRunId);

    default Future<Void> pushPing(DeliveryOptions options, Bus bus, Object clientRunId) {
        return push(ClientPushBusAddressesSharedByBothClientAndServer.PUSH_PING_CLIENT_LISTENER_SERVICE_ADDRESS, "Push ping to client " + clientRunId, options, bus, clientRunId);
    }

    void clientIsLive(Object clientRunId);

    /**
     * Records invariant per-connection metadata (client build version, PWA mode, device profile, BO/FO
     * app) on an already-registered connected client, for the /monitor distributions. No-op when the
     * client isn't registered yet (it will be re-supplied on the next live tick) — never creates an
     * entry. Null clientVersion/pwa/clientProfile/backoffice are ignored (kept as previously known);
     * {@code userId} reflects the current login and is stored as-is on each tick (it changes on
     * login/logout).
     */
    default void setClientMetadata(Object clientRunId, Object userId, String clientVersion, Boolean pwa, String clientProfile, Boolean backoffice) {
    }

    /** Snapshot of the currently-connected clients' invariant metadata, for the /monitor distributions. */
    default List<PushClientMetadata> snapshotConnectedClients() {
        return Collections.emptyList();
    }

    /**
     * Returns the number of clients currently registered on this push server (i.e. clients the
     * server has pushed to at least once — in practice every connected client, since the server
     * pushes authorizations to each of them on connection). For monitoring purposes.
     */
    default int getPushClientsCount() {
        return 0;
    }

    void addUnresponsivePushClientListener(UnresponsivePushClientListener listener);

    void removeUnresponsivePushClientListener(UnresponsivePushClientListener listener);

}
