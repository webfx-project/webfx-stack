package dev.webfx.stack.push.server.spi.impl.simple;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.com.bus.call.BusCallService;
import dev.webfx.stack.push.ClientPushBusAddressesSharedByBothClientAndServer;
import dev.webfx.stack.push.server.PushClientMetadata;
import dev.webfx.stack.push.server.UnresponsivePushClientListener;
import dev.webfx.stack.push.server.spi.PushServerServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Bruno Salmon
 */
public final class SimplePushServerServiceProvider implements PushServerServiceProvider {

    private final static long PING_PUSH_PERIOD_MS = 20_000; // Should be lower than client WebSocketBusOptions.pingInterval (which is set to 30_000 at the time of this writing)

    private final static boolean LOG_PUSH = true;

    // ConcurrentHashMap: the monitor's snapshotConnectedClients() iterates this map while other event
    // loops mutate it (push() creates entries, pushFailed() removes them) — a plain HashMap would throw
    // ConcurrentModificationException under real multi-client load (breaking getMonitorInfo).
    private final Map<Object /*clientRunId*/, PushClientInfo> pushClientInfos = new ConcurrentHashMap<>();
    private final List<UnresponsivePushClientListener> unresponsivePushClientListeners = new ArrayList<>();

    @Override
    public <T> Future<T> push(String clientServiceAddress, Object javaArgument, DeliveryOptions options, Bus bus, Object clientRunId) {
        PushClientInfo pushClientInfo = getOrCreatePushClientInfo(clientRunId);
        String clientBusCallServiceAddress = ClientPushBusAddressesSharedByBothClientAndServer.computeClientBusCallServiceAddress(clientRunId);
        pushClientInfo.touchCalled();
        return BusCallService.<T>call(clientBusCallServiceAddress, clientServiceAddress, javaArgument, options, bus)
            .onComplete(ar -> {
                pushClientInfo.touchReceived(ar.cause());
                if (LOG_PUSH) {
                    if (ar.succeeded())
                        Console.log("✅ Push " + clientBusCallServiceAddress + " -> " + clientServiceAddress + " was successful");
                    else
                        Console.log("❌ Push " + clientBusCallServiceAddress + " -> " + clientServiceAddress + " failed: " + ar.cause());
                }
            });
    }

    @Override
    public void clientIsLive(Object clientRunId) {
        PushClientInfo pushClientInfo = pushClientInfos.get(clientRunId);
        if (pushClientInfo != null)
            pushClientInfo.rescheduleNextPing();
    }

    @Override
    public void setClientMetadata(Object clientRunId, Object userId, String clientVersion, Boolean pwa, String clientProfile, Boolean backoffice) {
        // Attach to an already-registered client only; a not-yet-registered one gets it on a later
        // live tick (the values are re-supplied from the session each time).
        PushClientInfo pushClientInfo = pushClientInfos.get(clientRunId);
        if (pushClientInfo != null) {
            // userId reflects the current login — store it as-is each tick (it changes on login/logout).
            pushClientInfo.userId = userId;
            // version/pwa/profile/backoffice are invariant; keep the last known value if a tick supplies null.
            if (clientVersion != null)
                pushClientInfo.clientVersion = clientVersion;
            if (pwa != null)
                pushClientInfo.pwa = pwa;
            if (clientProfile != null)
                pushClientInfo.clientProfile = clientProfile;
            if (backoffice != null)
                pushClientInfo.backoffice = backoffice;
        }
    }

    @Override
    public List<PushClientMetadata> snapshotConnectedClients() {
        List<PushClientMetadata> snapshot = new ArrayList<>(pushClientInfos.size());
        for (PushClientInfo info : pushClientInfos.values())
            snapshot.add(new PushClientMetadata(info.userId, info.clientVersion, info.pwa, info.clientProfile, info.backoffice));
        return snapshot;
    }

    @Override
    public int getPushClientsCount() {
        return pushClientInfos.size();
    }

    @Override
    public void addUnresponsivePushClientListener(UnresponsivePushClientListener listener) {
        unresponsivePushClientListeners.add(listener);
    }

    @Override
    public void removeUnresponsivePushClientListener(UnresponsivePushClientListener listener) {
        unresponsivePushClientListeners.remove(listener);
    }

    private void firePushClientDisconnected(Object clientRunId) {
        Console.log("Push client disconnected: clientRunId = " + clientRunId);
        for (UnresponsivePushClientListener listener : unresponsivePushClientListeners)
            listener.onUnresponsivePushClient(clientRunId);
    }

    private void pushFailed(Object clientRunId) {
        pushClientInfos.remove(clientRunId);
        firePushClientDisconnected(clientRunId);
    }

    private PushClientInfo getOrCreatePushClientInfo(Object clientRunId) {
        return pushClientInfos.computeIfAbsent(clientRunId, PushClientInfo::new);
    }

    final class PushClientInfo {
        final Object clientRunId;
        int pendingCalls;
        long lastCallTime;
        long lastResultReceivedTime;
        Scheduled pingScheduled;
        // Session facts for the /monitor page. userId = the current login (updated each live tick);
        // clientVersion/pwa/clientProfile/backoffice are invariant (null until the client reports them).
        Object userId;
        String clientVersion;
        Boolean pwa;
        String clientProfile;
        Boolean backoffice; // TRUE = back-office app, FALSE = front-office app, null = unknown

        PushClientInfo(Object clientRunId) {
            this.clientRunId = clientRunId;
        }

        void touchCalled() {
            pendingCalls++;
            lastCallTime = now();
            rescheduleNextPing();
        }

        void touchReceived(Throwable error) {
            pendingCalls--;
            lastResultReceivedTime = now();
            if (error == null)
                rescheduleNextPing();
            else {
                cancelNextPing();
                pushFailed(clientRunId);
            }
        }

        void rescheduleNextPing() {
            cancelNextPing();
            pingScheduled = Scheduler.scheduleDelay(PING_PUSH_PERIOD_MS, this::pushPingNow);
        }

        void cancelNextPing() {
            if (pingScheduled != null)
                pingScheduled.cancel();
            pingScheduled = null;

        }

        void pushPingNow() {
            pushPing(new DeliveryOptions(), BusService.bus(), clientRunId);
        }
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}