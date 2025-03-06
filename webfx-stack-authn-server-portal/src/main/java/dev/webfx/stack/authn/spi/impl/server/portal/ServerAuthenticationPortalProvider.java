package dev.webfx.stack.authn.spi.impl.server.portal;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.util.FutureBroadcaster;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.service.MultipleServiceProviders;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.server.gateway.spi.ServerAuthenticationGateway;
import dev.webfx.stack.authn.spi.AuthenticationServiceProvider;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

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
            Console.log("👮👮 Joining same user verification broadcaster");
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

    @Override
    public Future<UserClaims> getUserClaims() {
        for (ServerAuthenticationGateway gateway : getGateways()) {
            boolean accepts = gateway.acceptsUserId();
            if (accepts)
                return gateway.getUserClaims();
        }
        return Future.failedFuture("getUserClaims() failed on server authentication portal because no server gateway accepted UserId " + ThreadLocalStateHolder.getUserId());
    }

    @Override
    public Future<?> updateCredentials(Object updateCredentialsArgument) {
        for (ServerAuthenticationGateway gateway : getGateways()) {
            //boolean acceptsUserId = gateway.acceptsUserId();
            boolean acceptsArgument = gateway.acceptsUpdateCredentialsArgument(updateCredentialsArgument);
            if (/*acceptsUserId &&*/ acceptsArgument)
                return gateway.updateCredentials(updateCredentialsArgument);
        }
        return Future.failedFuture("No server authentication gateway found accepting credentials update " + updateCredentialsArgument);
    }

    @Override
    public Future<Void> logout() {
        for (ServerAuthenticationGateway gateway : getGateways()) {
            boolean accepts = gateway.acceptsUserId();
            if (accepts)
                return gateway.logout();
        }
        return Future.failedFuture("logout() failed on server authentication portal because no server gateway accepted UserId " + ThreadLocalStateHolder.getUserId());
    }

}
