package dev.webfx.stack.push.server.spi;

import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.push.server.PushClientMetadata;
import dev.webfx.stack.push.server.UnresponsivePushClientListener;
import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.push.ClientPushBusAddressesSharedByBothClientAndServer;

import java.util.Collection;
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
     * Which app a connected client reported itself to be — TRUE back office, FALSE front office, null
     * where this client has not said or is not registered here.
     *
     * <p>For the one case that needs to mint a session FOR A CLIENT OTHER THAN THE CALLER: the magic-link
     * requester push, which signs in the tab that asked for a link while running inside the request of
     * the tab that clicked it. Everywhere else the flag is captured from the caller's own connection at
     * method entry, and this must not be used to second-guess that — it answers about a third party.
     *
     * <p>Null is "do not know", and a caller must not read it as the front office. Deciding an audience
     * from a guess is how a session ends up stamped for an app it is not, which outlives the guess: the
     * claim is signed and cannot be corrected afterwards.
     *
     * <p>It is as trustworthy as the flag anywhere else — the client asserted it — and no less: the same
     * assertion, by the same client, recorded when it last spoke rather than read from the current
     * thread. The thread is the wrong place to read it here, because the thread belongs to the OTHER
     * client.
     */
    default Boolean getClientBackoffice(Object clientRunId) {
        return null;
    }

    /**
     * Records which session family a connected client's verified token belongs to, so a revocation of that
     * family can reach the client at once instead of waiting for its next message.
     *
     * <p>Deliberately NOT part of {@link #setClientMetadata}: that describes a client for the monitor, and
     * a family id is a security routing fact that has no business appearing in a snapshot somebody
     * browses. Re-supplied on every live tick so it follows a login and a token rotation without anything
     * having to invalidate it — but NOT, unlike the userId beside it, a logout: the syncer records a
     * family only when a token verified and never clears one, so a logged-out connection keeps naming the
     * family it last held. That is deliberate and safe (pushing a logout at a client that is already
     * logged out does nothing) and it is what keeps a connection reachable on a message that carried no
     * token. Null therefore means "never held a readable token", not "logged out".
     *
     * <p><b>{@code ownerSessionId} is not decoration.</b> The runId is chosen by the client and nothing
     * authenticates it; the family is taken from a token this server verified. Believing the pair would let
     * a caller file its own genuine family under somebody else's runId and then sign that person out by
     * ending its own session. So the first session to claim a runId owns it, and a claim arriving from a
     * different session is refused — the refused client simply keeps finding out at its next message, which
     * is where it was before any of this existed.
     *
     * <p>No-op when the client isn't registered yet — the next live tick supplies it again.
     */
    default void setClientSessionFamily(Object clientRunId, String sessionFamilyId, String ownerSessionId) {
    }

    /**
     * The run ids of connected clients whose token belongs to one of these families — whom to tell that
     * their session is over. Empty when nothing matches, which is the ordinary case: the client is usually
     * connected to the OTHER instance.
     */
    default List<Object> snapshotClientRunIdsOfFamilies(Collection<String> sessionFamilyIds) {
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
