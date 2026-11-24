package dev.webfx.stack.authn.spi.impl.server.portal;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.util.FutureBroadcaster;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.service.MultipleServiceProviders;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.server.gateway.spi.ServerAuthenticationGateway;
import dev.webfx.stack.authn.server.gateway.spi.impl.ServerAuthenticationGatewayBase;
import dev.webfx.stack.authn.spi.AuthenticationServiceProvider;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import dev.webfx.stack.session.token.SessionTokenService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public class ServerAuthenticationPortalProvider implements AuthenticationServiceProvider {

    private static List<ServerAuthenticationGateway> getGateways() {
        return MultipleServiceProviders.getProviders(ServerAuthenticationGateway.class, () -> ServiceLoader.load(ServerAuthenticationGateway.class));
    }

    public ServerAuthenticationPortalProvider() { // Called first time on server start through AuthenticationService.getProvider() call in AuthenticateMethodEndpoint.
        // We instantiate the gateways (such as Google, Facebook, etc...) and call their boot() method, which may do
        // some initialisation (ex: fetching Facebook application access token). This must be done as soon as possible,
        // i.e. on server start.
        for (ServerAuthenticationGateway gateway : getGateways())
            gateway.boot();
    }

    @Override
    public Future<?> authenticate(Object userCredentials) {
        for (ServerAuthenticationGateway gateway : getGateways()) {
            boolean accepts = gateway.acceptsUserCredentials(userCredentials);
            if (accepts)
                return gateway.authenticate(userCredentials);
        }
        // The TYPE, never the object: credentials carry passwords, and this message travels back to the client
        return Future.failedFuture("No server authentication gateway found accepting credentials " + typeNameOf(userCredentials));
    }

    private final Map<Object, FutureBroadcaster<?>> userVerificationBroadcasters = new HashMap<>();

    @Override
    public Future<?> verifyAuthenticated() {
        Object userId = ThreadLocalStateHolder.getUserId();
        FutureBroadcaster<?> userVerificationBroadcaster = userVerificationBroadcasters.get(userId);
        if (userVerificationBroadcaster != null) {
            Console.log("👮 Joining same user verification broadcaster");
            return userVerificationBroadcaster.newClient();
        }
        for (ServerAuthenticationGateway gateway : getGateways()) {
            boolean accepts = gateway.acceptsUserId();
            if (accepts) {
                userVerificationBroadcaster = new FutureBroadcaster<>(() -> gateway.verifyAuthenticated()
                        .map(uid -> {
                            userVerificationBroadcasters.remove(userId);
                            return uid;
                        }));
                userVerificationBroadcasters.put(userId, userVerificationBroadcaster);
                return userVerificationBroadcaster.newClient();
            }
        }
        return Future.failedFuture("verifyAuthenticated() failed on server authentication portal because no server gateway accepted UserId " + ThreadLocalStateHolder.getUserId());
    }

    // The same user claims are requested several times within seconds on each (re)connection (authorization
    // push + client getUserDetails + re-verification), and by ALL clients at once after a deploy. A pending
    // future dedups concurrent requests; a completed one is served for a short TTL. Claims change only through
    // updateCredentials(), which evicts the caller's entry.
    private static final long USER_CLAIMS_CACHE_TTL_MILLIS = 60_000;

    private record CachedClaims(Future<UserClaims> future, long computedAtMillis) {}
    private final Map<Object, CachedClaims> userClaimsCache = new HashMap<>();

    @Override
    public Future<UserClaims> getUserClaims() {
        Object userId = ThreadLocalStateHolder.getUserId();
        CachedClaims cached = userClaimsCache.get(userId);
        long now = System.currentTimeMillis();
        if (cached != null && (!cached.future.isComplete() || cached.future.succeeded() && now - cached.computedAtMillis < USER_CLAIMS_CACHE_TTL_MILLIS))
            return cached.future;
        if (userClaimsCache.size() > 100) // expired entries are otherwise only replaced in place
            userClaimsCache.values().removeIf(c -> c.future.isComplete() && now - c.computedAtMillis >= USER_CLAIMS_CACHE_TTL_MILLIS);
        for (ServerAuthenticationGateway gateway : getGateways()) {
            boolean accepts = gateway.acceptsUserId();
            if (accepts) {
                Future<UserClaims> future = gateway.getUserClaims();
                userClaimsCache.put(userId, new CachedClaims(future, now));
                // A failed load must not be served to subsequent callers
                future.onFailure(e -> {
                    CachedClaims current = userClaimsCache.get(userId);
                    if (current != null && current.future == future)
                        userClaimsCache.remove(userId);
                });
                return future;
            }
        }
        return Future.failedFuture("getUserClaims() failed on server authentication portal because no server gateway accepted UserId " + ThreadLocalStateHolder.getUserId());
    }

    @Override
    public Future<?> updateCredentials(Object updateCredentialsArgument) {
        for (ServerAuthenticationGateway gateway : getGateways()) {
            //boolean acceptsUserId = gateway.acceptsUserId();
            boolean acceptsArgument = gateway.acceptsUpdateCredentialsArgument(updateCredentialsArgument);
            if (/*acceptsUserId &&*/ acceptsArgument) {
                // Credentials updates change the claims (email/username/phone), so the cached ones are evicted
                Object userId = ThreadLocalStateHolder.getUserId();
                return gateway.updateCredentials(updateCredentialsArgument)
                        .map(result -> {
                            userClaimsCache.remove(userId);
                            return result;
                        });
            }
        }
        return Future.failedFuture("No server authentication gateway found accepting credentials update " + typeNameOf(updateCredentialsArgument));
    }

    @Override
    public Future<Void> logout() {
        return logoutWith(getGateways());
    }

    /** Package-private so LogoutCheck can hand it a gateway of its own; everything else goes through logout(). */
    static Future<Void> logoutWith(List<ServerAuthenticationGateway> gateways) {
        // Ending the session family comes FIRST, and it happens whatever the gateway then makes of the
        // logout. It is the only part that ends the session for anyone holding a COPY of the token rather
        // than only for the device that asked; the gateway's own logout is about that device. Reading it
        // here rather than in each gateway also means a gateway added later cannot forget it.
        //
        // Deliberately not conditional on a gateway accepting the userId: a caller whose identity no gateway
        // recognises any more is exactly the one whose family most wants ending, and the failure below must
        // not be the thing that skips it.
        //
        // The caller's state is captured HERE and put back around the gateway, because revoking is a database
        // round trip and the thread-local state is restored the moment the synchronous part of this call
        // returns. Everything the gateway step does reads that state: acceptsUserId() asks who is calling, and
        // the gateways' logout pushes to the runId it holds. Read late, both see nobody — which is what
        // shipped first: every logout ended the family and then failed with "no gateway accepted UserId null".
        // The device that asked was never told, its server session went on naming the user, and a renewed
        // token still waiting for delivery was stapled to its next push and signed the page straight back in.
        Object callerState = ThreadLocalStateHolder.getThreadLocalState();
        return SessionTokenService.revokeCurrentSessionFamily()
            .compose(ignored -> ThreadLocalStateHolder.runWithState(callerState, () -> logoutThroughGateway(gateways)));
    }

    /**
     * Ends every session of the caller except this one, and answers how many.
     *
     * <p>Revoking is the whole of the enforcement. Every instance refuses those sessions on sight once it
     * knows, and refuses them at renewal even if it never does, so a device stops at its next message.
     *
     * <p><b>The devices are now told at once, and the distinction matters.</b> An earlier version of this
     * did NOT push, because a push is addressed by run id while a revocation is scoped by family, and
     * those do not line up: tabs of one browser share a family but each holds its own run id, so pushing
     * to "every run id of this person except the caller's" would sign out the caller's OTHER TABS, whose
     * session was deliberately spared. That objection was answered by making the push family-aware rather
     * than by-person — the push server now records which family each connected client holds — so what is
     * told is exactly the set that was revoked, and the caller's spared family is not in it. See
     * RevokedFamilyLogoutPush.
     *
     * <p>Do not read that as a stronger guarantee than it is. The push is a courtesy to a cooperating
     * client; revoking is still the whole of the enforcement, and a device that ignores the message, or
     * is offline, or is connected to the other instance, stops at its next message as before.
     */
    @Override
    public Future<Integer> revokeOtherSessions() {
        return SessionTokenService.revokeOtherSessionsOfCurrentUser().map(List::size);
    }

    /**
     * Must run with the caller's state on the thread — see logoutWith().
     *
     * <p><b>A caller nobody recognises is already logged out, and is told so.</b> That is the common way
     * to arrive here, not an exotic one: the token layer refuses an identity claim whose token is missing
     * or expired, and what reaches this method is the logout sentinel, which no gateway accepts. A
     * back-office session is capped at a day, so every staff member who leaves a tab open overnight and
     * then clicks Logout is this caller.
     *
     * <p>This used to answer with a failure, and that failure was the bug. Logging out is the one
     * operation that must work when an identity is no longer accepted — it is what somebody does ABOUT
     * that. The part of logout that ends anything has already run above, unconditionally and for exactly
     * this caller's sake. Succeeding also makes logout idempotent, which it should be: doing it twice is
     * not an error.
     *
     * <p><b>And the caller is told, which succeeding alone did not do.</b> Telling the client is the whole
     * of what a gateway's logout does, so with no gateway to do it, nobody did: the first version of this
     * fix stopped the call failing and left the back office still showing the user signed in, because the
     * Java client clears nothing of its own and waits to be told. The server does send that message when
     * it first refuses a claim — but once, addressed by runId, with no retry, and the session holds the
     * sentinel from then on so the quiet path takes over and it is never sent again. A logout is the
     * moment the client is certainly listening, so it is the moment worth sending it again.
     *
     * <p>What the failure cost, found on staging on 2026-10-13: the client is told its logout failed and
     * keeps its stored session. The React client clears local state in a {@code finally} and escapes it;
     * the Java client's logout is a bare bus call that clears nothing and waits for a server push that
     * never comes — so the legacy back office stayed signed in across reloads, with every other call
     * failing too, and the only way out was clearing the browser's site data by hand.
     */
    private static Future<Void> logoutThroughGateway(List<ServerAuthenticationGateway> gateways) {
        for (ServerAuthenticationGateway gateway : gateways) {
            boolean accepts = gateway.acceptsUserId();
            if (accepts)
                return gateway.logout();
        }
        // Still a failure when there is no gateway AT ALL: that is a server missing its authentication
        // providers, not a caller who is already logged out, and treating the two alike would let every
        // logout on a misconfigured server report success while doing nothing.
        if (gateways.isEmpty())
            return Future.failedFuture("logout() failed on server authentication portal because no server authentication gateway is registered at all");
        // The push is a courtesy and must never decide the outcome: a client that has gone, or a push
        // service that is not registered at all, would otherwise fail the logout again — which is the bug
        // this method exists to fix, reintroduced one line further down.
        try {
            return ServerAuthenticationGatewayBase.pushLogoutMessageToUnrecognisedCaller()
                .otherwise(e -> {
                    Console.log("⚠️ Logged out a caller no gateway recognises, but could not tell them: " + e);
                    return null;
                });
        } catch (Exception e) {
            Console.log("⚠️ Logged out a caller no gateway recognises, but could not tell them: " + e);
            return Future.succeededFuture();
        }
    }


    /** A credentials argument named by its type — safe to put in a message, unlike its toString(). */
    private static String typeNameOf(Object argument) {
        return argument == null ? "null" : argument.getClass().getSimpleName();
    }
}
