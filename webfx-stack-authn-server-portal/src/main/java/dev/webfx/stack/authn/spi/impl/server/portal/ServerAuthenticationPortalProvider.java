package dev.webfx.stack.authn.spi.impl.server.portal;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.spi.AuthenticationServiceProvider;
import dev.webfx.stack.authn.spi.impl.server.gateway.ServerAuthenticationGatewayProvider;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public class ServerAuthenticationPortalProvider implements AuthenticationServiceProvider {

    @Override
    public Future<?> authenticate(Object userCredentials) {
        Iterator<ServerAuthenticationGatewayProvider> it = getGatewayProviders().iterator();
        if (it.hasNext()) {
            ServerAuthenticationGatewayProvider gatewayProvider = it.next();
            if (gatewayProvider.acceptsUserCredentials(userCredentials))
                return gatewayProvider.authenticate(userCredentials);
        }
        return Future.failedFuture("No server authentication gateway found accepting credentials " + userCredentials);
    }

    @Override
    public Future<?> verifyAuthenticated() {
        Iterator<ServerAuthenticationGatewayProvider> it = getGatewayProviders().iterator();
        if (it.hasNext())
            return it.next().verifyAuthenticated();
        return Future.failedFuture("No server authentication gateway found!");
    }

    @Override
    public Future<UserClaims> getUserClaims() {
        Iterator<ServerAuthenticationGatewayProvider> it = getGatewayProviders().iterator();
        if (it.hasNext())
            return it.next().getUserClaims();
        return Future.failedFuture("No server authentication gateway found!");
    }

    @Override
    public Future<Void> logout() {
        Iterator<ServerAuthenticationGatewayProvider> it = getGatewayProviders().iterator();
        if (it.hasNext())
            return it.next().logout();
        return Future.failedFuture("No server authentication gateway found!");
    }

    private ServiceLoader<ServerAuthenticationGatewayProvider> getGatewayProviders() {
        return ServiceLoader.load(ServerAuthenticationGatewayProvider.class);
    }

}
