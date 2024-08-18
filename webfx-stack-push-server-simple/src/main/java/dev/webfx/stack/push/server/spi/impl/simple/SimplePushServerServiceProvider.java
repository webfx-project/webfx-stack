package dev.webfx.stack.push.server.spi.impl.simple;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.push.server.UnresponsivePushClientListener;
import dev.webfx.stack.push.server.spi.PushServerServiceProvider;
import dev.webfx.stack.push.ClientPushBusAddressesSharedByBothClientAndServer;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.com.bus.call.BusCallService;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class SimplePushServerServiceProvider implements PushServerServiceProvider {

    private final static long PING_PUSH_PERIOD_MS = 20_000; // Should be lower than client WebSocketBusOptions.pingInterval (which is set to 30_000 at the time of this writing)

    private final static boolean LOG_PUSH = true;

    private final Map<Object /*clientRunId*/, PushClientInfo> pushClientInfos = new HashMap<>();
    private final List<UnresponsivePushClientListener> unresponsivePushClientListeners = new ArrayList<>();

    @Override
    public <T> Future<T> push(String clientServiceAddress, Object javaArgument, DeliveryOptions options, Bus bus, Object clientRunId) {
        Promise<T> promise = Promise.promise();
        PushClientInfo pushClientInfo = getOrCreatePushClientInfo(clientRunId);
        String clientBusCallServiceAddress = ClientPushBusAddressesSharedByBothClientAndServer.computeClientBusCallServiceAddress(clientRunId);
        if (LOG_PUSH)
            Console.log("Pushing " + clientBusCallServiceAddress + " -> " + clientServiceAddress);
        pushClientInfo.touchCalled();
        BusCallService.<T>call(clientBusCallServiceAddress, clientServiceAddress, javaArgument, options, bus)
            .onComplete(ar -> {
                pushClientInfo.touchReceived(ar.cause());
                if (ar.failed())
                    pushFailed(clientRunId);
                promise.handle(ar);
            });
        return promise.future();
    }

    @Override
    public void clientIsLive(Object clientRunId) {
        PushClientInfo pushClientInfo = pushClientInfos.get(clientRunId);
        if (pushClientInfo != null)
            pushClientInfo.rescheduleNextPing();
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
        PushClientInfo pushClientInfo = pushClientInfos.get(clientRunId);
        if (pushClientInfo == null)
            pushClientInfos.put(clientRunId, pushClientInfo = new PushClientInfo(clientRunId));
        return pushClientInfo;
    }

    final class PushClientInfo {
        final Object clientRunId;
        int pendingCalls;
        long lastCallTime;
        long lastResultReceivedTime;
        Scheduled pingScheduled;

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