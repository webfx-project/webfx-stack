package dev.webfx.stack.authn.spi.impl.server.gateway.google;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.logout.server.LogoutPush;
import dev.webfx.stack.authn.spi.AuthenticatorInfo;
import dev.webfx.stack.authn.spi.impl.server.gateway.ServerAuthenticationGatewayProvider;

/**
 * @author Bruno Salmon
 */
public final class GoogleServerAuthenticationGatewayProvider implements ServerAuthenticationGatewayProvider {


    @Override
    public AuthenticatorInfo getAuthenticatorInfo() {
        return null;
    }

    @Override
    public boolean acceptsUserCredentials(Object userCredentials) {
        return false;
    }

    @Override
    public Future<Object> authenticate(Object userId) {
        return Future.failedFuture("Google authenticate() is not yet implemented");
    }

    @Override
    public Future<?> verifyAuthenticated() {
        return Future.failedFuture("Google verifyAuthenticated() is not yet implemented");
    }

    @Override
    public Future<UserClaims> getUserClaims() {
        return Future.failedFuture("Google getUserClaims() is not yet implemented");
    }

    @Override
    public Future<Void> logout() {
        return LogoutPush.pushLogoutMessageToClient();
    }
}
