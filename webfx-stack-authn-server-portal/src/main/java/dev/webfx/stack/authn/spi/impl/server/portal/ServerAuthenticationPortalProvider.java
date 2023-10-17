package dev.webfx.stack.authn.spi.impl.server.portal;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.serviceloader.MultipleServiceProviders;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.server.gateway.spi.ServerAuthenticationGatewayProvider;
import dev.webfx.stack.authn.spi.AuthenticationServiceProvider;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public class ServerAuthenticationPortalProvider implements AuthenticationServiceProvider {

    private static List<ServerAuthenticationGatewayProvider> getGatewayProviders() {
        return MultipleServiceProviders.getProviders(ServerAuthenticationGatewayProvider.class, () -> ServiceLoader.load(ServerAuthenticationGatewayProvider.class));
    }

    public ServerAuthenticationPortalProvider() { // Called first time on server start through AuthenticationService.getProvider() call in AuthenticateMethodEndpoint.
        // We instantiate the gateways (such as Google, Facebook, etc...) and call their boot() method, which may do
        // some initialisation (ex: fetching Facebook application access token). This must be done as soon as possible,
        // i.e. on server start.
        for (ServerAuthenticationGatewayProvider gatewayProvider : getGatewayProviders())
            gatewayProvider.boot();
    }

    @Override
    public Future<?> authenticate(Object userCredentials) {
        for (ServerAuthenticationGatewayProvider gatewayProvider : getGatewayProviders()) {
            boolean accepts = gatewayProvider.acceptsUserCredentials(userCredentials);
            if (accepts)
                return gatewayProvider.authenticate(userCredentials);
        }
        return Future.failedFuture("No server authentication gateway found accepting credentials " + userCredentials);
    }

    @Override
    public Future<?> verifyAuthenticated() {
        for (ServerAuthenticationGatewayProvider gatewayProvider : getGatewayProviders()) {
            boolean accepts = gatewayProvider.acceptsUserId();
            if (accepts)
                return gatewayProvider.verifyAuthenticated();
        }
        return Future.failedFuture("verifyAuthenticated() failed on server authentication portal because no server gateway accepted UserId " + ThreadLocalStateHolder.getUserId());
    }

    @Override
    public Future<UserClaims> getUserClaims() {
        for (ServerAuthenticationGatewayProvider gatewayProvider : getGatewayProviders()) {
            boolean accepts = gatewayProvider.acceptsUserId();
            if (accepts)
                return gatewayProvider.getUserClaims();
        }
        return Future.failedFuture("getUserClaims() failed on server authentication portal because no server gateway accepted UserId " + ThreadLocalStateHolder.getUserId());
    }

    @Override
    public Future<Void> logout() {
        for (ServerAuthenticationGatewayProvider gatewayProvider : getGatewayProviders()) {
            boolean accepts = gatewayProvider.acceptsUserId();
            if (accepts)
                return gatewayProvider.logout();
        }
        return Future.failedFuture("logout() failed on server authentication portal because no server gateway accepted UserId " + ThreadLocalStateHolder.getUserId());
    }

}
