package dev.webfx.stack.authn.spi.impl.server.portal;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.spi.AuthenticationServiceProvider;
import dev.webfx.stack.authn.spi.impl.server.gateway.ServerAuthenticationGatewayProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public class ServerAuthenticationPortalProvider implements AuthenticationServiceProvider {

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
        return Future.failedFuture("No server authentication gateway found!");
    }

    @Override
    public Future<UserClaims> getUserClaims() {
        for (ServerAuthenticationGatewayProvider gatewayProvider : getGatewayProviders()) {
            boolean accepts = gatewayProvider.acceptsUserId();
            if (accepts)
                return gatewayProvider.getUserClaims();
        }
        return Future.failedFuture("No server authentication gateway found!");
    }

    @Override
    public Future<Void> logout() {
        for (ServerAuthenticationGatewayProvider gatewayProvider : getGatewayProviders()) {
            boolean accepts = gatewayProvider.acceptsUserId();
            if (accepts)
                return gatewayProvider.logout();
        }
        return Future.failedFuture("No server authentication gateway found!");
    }

    private ServiceLoader<ServerAuthenticationGatewayProvider> getGatewayProviders() {
        return ServiceLoader.load(ServerAuthenticationGatewayProvider.class);
    }

}
