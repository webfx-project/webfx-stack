package dev.webfx.stack.authn.spi.impl.server.gateway.facebook;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.logout.server.LogoutPush;
import dev.webfx.stack.authn.spi.AuthenticatorInfo;
import dev.webfx.stack.authn.spi.impl.server.gateway.ServerAuthenticationGatewayProvider;

/**
 * @author Bruno Salmon
 */
public final class FacebookServerAuthenticationGatewayProvider implements ServerAuthenticationGatewayProvider {


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
        return Future.failedFuture("Facebook authenticate() is not yet implemented");
    }

    @Override
    public boolean acceptsUserId() {
        return false;
    }

    @Override
    public Future<?> verifyAuthenticated() {
        return Future.failedFuture("Facebook verifyAuthenticated() is not yet implemented");
    }

    @Override
    public Future<UserClaims> getUserClaims() {
        return Future.failedFuture("Facebook getUserClaims() is not yet implemented");
    }

    @Override
    public Future<Void> logout() {
        return LogoutPush.pushLogoutMessageToClient();
    }
}
