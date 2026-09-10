package dev.webfx.stack.authn.spi.impl.server.portal;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.util.FutureBroadcaster;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.service.MultipleServiceProviders;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.server.gateway.spi.ServerAuthenticationGateway;
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
        return Future.failedFuture("No server authentication gateway found accepting credentials " + userCredentials);
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
        return Future.failedFuture("No server authentication gateway found accepting credentials update " + updateCredentialsArgument);
    }

    @Override
    public Future<Void> logout() {
        // Ending the session family comes FIRST, and it happens whatever the gateway then makes of the
        // logout. It is the only part that ends the session for anyone holding a COPY of the token rather
        // than only for the device that asked; the gateway's own logout is about that device. Reading it
        // here rather than in each gateway also means a gateway added later cannot forget it.
        //
        // Deliberately not conditional on a gateway accepting the userId: a caller whose identity no gateway
        // recognises any more is exactly the one whose family most wants ending, and the failure below must
        // not be the thing that skips it.
        return SessionTokenService.revokeCurrentSessionFamily()
            .compose(ignored -> {
                for (ServerAuthenticationGateway gateway : getGateways()) {
                    boolean accepts = gateway.acceptsUserId();
                    if (accepts)
                        return gateway.logout();
                }
                return Future.failedFuture("logout() failed on server authentication portal because no server gateway accepted UserId " + ThreadLocalStateHolder.getUserId());
            });
    }

}
