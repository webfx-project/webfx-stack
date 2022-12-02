package dev.webfx.stack.authn.spi.impl.server.gateway;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.spi.AuthenticatorInfo;

public interface ServerAuthenticationGatewayProvider {

    AuthenticatorInfo getAuthenticatorInfo();

    boolean acceptsUserCredentials(Object userCredentials);

    Future<?> authenticate(Object userCredentials);

    Future<?> verifyAuthenticated();

    Future<UserClaims> getUserClaims();

    Future<Void> logout();

}
