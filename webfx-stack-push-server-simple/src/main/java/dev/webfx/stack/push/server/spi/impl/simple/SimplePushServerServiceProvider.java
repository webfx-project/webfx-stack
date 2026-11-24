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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Bruno Salmon
 */
public final class SimplePushServerServiceProvider implements PushServerServiceProvider {

    private final static long PING_PUSH_PERIOD_MS = 20_000; // Should be lower than client WebSocketBusOptions.pingInterval (which is set to 30_000 at the time of this writing)

    private final static boolean LOG_PUSH = true;

    /**
     * When to ping a client again after a push to it failed, before giving it up. A single failure used to
     * give it up at once — and with it every query-push stream it had, server-side, without the client ever
     * being told: it kept its socket and its "Connected" status, and every live list on its page froze on
     * its last result (2026-10-10, a back office on a festival day: ten such failures in an hour, the last
     * one permanent). A failure is often a moment, not an end — the client's bus address is briefly
     * unregistered while its socket is replaced (NO_HANDLERS), or a reply goes astray on the old one — and
     * the client is back seconds later on the same runId. So it gets about two minutes to answer a ping.
     */
    final static long[] UNREACHABLE_PROBE_DELAYS_MS = { 2_000, 5_000, 10_000, 20_000, 30_000, 60_000 };

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
        if (pushClientInfo != null) {
            // A client we could not reach has just spoken — typically its first message on a new socket.
            // Ask it now rather than at the next probe, so its streams resume in a round trip.
            if (pushClientInfo.isUnreachable())
                pushClientInfo.probeNow();
            else
                pushClientInfo.rescheduleNextPing();
        }
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
    public void setClientSessionFamily(Object clientRunId, String sessionFamilyId, String ownerSessionId) {
        // Attach to an already-registered client only, like setClientMetadata — a not-yet-registered one
        // gets it on a later live tick, and creating an entry here would invent a push client that nothing
        // has ever pushed to.
        PushClientInfo pushClientInfo = pushClientInfos.get(clientRunId);
        if (pushClientInfo == null || ownerSessionId == null)
            return;
        // First claim wins, and every later one must come from the same session. The runId arrives in a
        // client message and nothing authenticates it, so a second session naming this runId is either a
        // caller trying to point somebody else's client at a family it controls — the reason this check
        // exists — or a genuine client whose session changed underneath it, which costs it only the prompt
        // logout and leaves it refused at its next message exactly as before.
        if (pushClientInfo.ownerSessionId == null) {
            pushClientInfo.ownerSessionId = ownerSessionId;
        } else if (!isFamilyClaimAllowed(pushClientInfo.ownerSessionId, ownerSessionId)) {
            if (!pushClientInfo.familyClaimRefusalReported) {
                pushClientInfo.familyClaimRefusalReported = true;
                // No runId, no session id, no family: naming them would put the very values an attacker
                // needs into the log, which is one of the few places a runId can actually be obtained.
                Console.log("🛡 Refused a second session's claim over a connected client's session family"
                            + " — the runId in a message is not proof of whose client it is");
            }
            return;
        }
        // Stored as supplied. Null means the session has no verified family — a client that has never
        // presented a readable token — and such a client is simply not addressable this way, which is
        // right: there is nothing to revoke.
        pushClientInfo.sessionFamilyId = sessionFamilyId;
    }

    /**
     * Whether a session may say which family a runId's client belongs to, given who claimed that runId first.
     *
     * <p>Its own named decision because it is the whole of the access control on prompt revocation, and
     * because the tempting "fix" for a client that lost its session — let the newcomer take over — is
     * precisely the attack: the runId travels in an unauthenticated client message, so a takeover is
     * available to anyone who learns one, and what they take over is the power to sign that client out.
     * Refusing costs the displaced client only its promptness.
     */
    static boolean isFamilyClaimAllowed(String currentOwnerSessionId, String claimingSessionId) {
        return currentOwnerSessionId == null || currentOwnerSessionId.equals(claimingSessionId);
    }

    @Override
    public List<Object> snapshotClientRunIdsOfFamilies(Collection<String> sessionFamilyIds) {
        if (sessionFamilyIds == null || sessionFamilyIds.isEmpty())
            return Collections.emptyList();
        // One walk for the whole batch rather than one per family: a bulk revocation is exactly when the
        // set is large, and it is also exactly when this must not become quadratic. A HashSet copy because
        // the caller's collection may be a list.
        Set<String> families = new HashSet<>(sessionFamilyIds);
        List<Object> runIds = new ArrayList<>();
        for (PushClientInfo info : pushClientInfos.values()) {
            String familyId = info.sessionFamilyId;
            if (familyId != null && families.contains(familyId))
                runIds.add(info.clientRunId);
        }
        return runIds;
    }

    @Override
    public Boolean getClientBackoffice(Object clientRunId) {
        // get, never getOrCreate: this asks a question about a client and must not bring one into
        // existence by asking. An absent entry and an entry that has not reported its app both answer
        // null, which is the same answer — we do not know.
        PushClientInfo pushClientInfo = clientRunId == null ? null : pushClientInfos.get(clientRunId);
        return pushClientInfo == null ? null : pushClientInfo.backoffice;
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

    private void firePushClientReachableAgain(Object clientRunId) {
        Console.log("Push client reachable again: clientRunId = " + clientRunId);
        for (UnresponsivePushClientListener listener : unresponsivePushClientListeners)
            listener.onPushClientReachableAgain(clientRunId);
    }

    private void pushFailed(PushClientInfo pushClientInfo) {
        // Only if the map still holds THIS entry — never evict a newer one created for the same runId.
        if (pushClientInfos.remove(pushClientInfo.clientRunId, pushClientInfo))
            firePushClientDisconnected(pushClientInfo.clientRunId);
    }

    /**
     * The delay before the given probe of an unreachable client, or -1 when the probes are used up and the
     * client must be given up. Its own method so the schedule can be checked without timers.
     */
    static long nextProbeDelay(int probeIndex) {
        return probeIndex < UNREACHABLE_PROBE_DELAYS_MS.length ? UNREACHABLE_PROBE_DELAYS_MS[probeIndex] : -1;
    }

    // Package-private, not private, so ClientBackofficeLookupCheck can register a client without a bus:
    // every other way in (push, clientIsLive, setClientMetadata) either needs one or deliberately refuses
    // to create an entry. Same reason isFamilyClaimAllowed is reachable from FamilyClaimCheck.
    PushClientInfo getOrCreatePushClientInfo(Object clientRunId) {
        return pushClientInfos.computeIfAbsent(clientRunId, PushClientInfo::new);
    }

    final class PushClientInfo {
        final Object clientRunId;
        int pendingCalls;
        long lastCallTime;
        long lastResultReceivedTime;
        // Volatile since a revocation announcement pushes to these clients from the revoking caller's (or
        // the poll's) event loop, not from the loop that owns the connection. Two loops rescheduling the
        // ping both read the old value, both cancel it harmlessly, and one assignment wins — leaving the
        // loser's timer unreachable and uncancellable, and since a ping push reschedules itself, that orphan
        // becomes a second ping chain running for this client for as long as it stays connected.
        volatile Scheduled pingScheduled;
        // Session facts for the /monitor page. userId = the current login (updated each live tick);
        // clientVersion/pwa/clientProfile/backoffice are invariant (null until the client reports them).
        Object userId;
        String clientVersion;
        Boolean pwa;
        String clientProfile;
        Boolean backoffice; // TRUE = back-office app, FALSE = front-office app, null = unknown
        // Which session family this client's verified token belongs to, so revoking that family can reach
        // it at once. Volatile: written on a live tick and read by whatever announces a revocation, which
        // is another event loop entirely. Null when the client presents no token this server could read.
        volatile String sessionFamilyId;
        // The session that claimed this runId first, and the only one allowed to say what family it belongs
        // to afterwards. The runId itself is client-chosen and unauthenticated, so without this a caller
        // could file its own family under another client's runId and sign that client out at will.
        volatile String ownerSessionId;
        // So a caller retrying the same refused claim on every message logs one line, not thousands.
        volatile boolean familyClaimRefusalReported;
        // Unreachable state, guarded by `this`: a push failed and the client has not answered since.
        // 0 = reachable. While unreachable the regular ping chain is suspended and probes run instead.
        private long unreachableSince;
        private int probeIndex;
        private Scheduled probeScheduled;
        // One probe at a time: a client that keeps sending messages while unreachable must not fire a ping
        // per message, which would spend the whole probe schedule in seconds.
        private boolean probeInFlight;

        PushClientInfo(Object clientRunId) {
            this.clientRunId = clientRunId;
        }

        void touchCalled() {
            pendingCalls++;
            lastCallTime = now();
            if (!isUnreachable())
                rescheduleNextPing();
        }

        void touchReceived(Throwable error) {
            pendingCalls--;
            lastResultReceivedTime = now();
            if (error == null) {
                if (markReachable())
                    firePushClientReachableAgain(clientRunId);
                rescheduleNextPing();
            } else
                markUnreachable(error);
        }

        synchronized boolean isUnreachable() {
            return unreachableSince != 0;
        }

        /** Enters the unreachable state on a failed push, once — later failures while in it change nothing. */
        private void markUnreachable(Throwable error) {
            synchronized (this) {
                if (unreachableSince != 0)
                    return;
                unreachableSince = now();
                probeIndex = 0;
            }
            cancelNextPing();
            Console.log("⚠ Push client unreachable, probing before giving it up: clientRunId = " + clientRunId + " (" + error.getMessage() + ")");
            scheduleNextProbe();
        }

        /** Leaves the unreachable state; true when the client WAS unreachable (its streams need resending). */
        private boolean markReachable() {
            synchronized (this) {
                if (unreachableSince == 0)
                    return false;
                unreachableSince = 0;
                cancelProbe();
                return true;
            }
        }

        private void scheduleNextProbe() {
            synchronized (this) {
                if (unreachableSince == 0)
                    return;
                long delay = nextProbeDelay(probeIndex++);
                if (delay >= 0) {
                    cancelProbe();
                    probeScheduled = Scheduler.scheduleDelay(delay, this::probeNow);
                    return;
                }
            }
            Console.log("Push client gave no answer to " + UNREACHABLE_PROBE_DELAYS_MS.length + " probes: clientRunId = " + clientRunId);
            pushFailed(this);
        }

        void probeNow() {
            synchronized (this) {
                if (unreachableSince == 0 || probeInFlight)
                    return;
                probeInFlight = true;
                cancelProbe();
            }
            // A success lands in touchReceived (reachable again); a failure schedules the next probe.
            pushPing(new DeliveryOptions(), BusService.bus(), clientRunId)
                .onComplete(ar -> {
                    synchronized (this) {
                        probeInFlight = false;
                    }
                    if (ar.failed())
                        scheduleNextProbe();
                });
        }

        private void cancelProbe() {
            if (probeScheduled != null)
                probeScheduled.cancel();
            probeScheduled = null;
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