package dev.webfx.stack.authn.server.gateway.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.spi.AuthenticatorInfo;

public interface ServerAuthenticationGatewayProvider {

    default void boot() {}

    AuthenticatorInfo getAuthenticatorInfo();

    boolean acceptsUserCredentials(Object userCredentials);

    Future<?> authenticate(Object userCredentials);

    boolean acceptsUserId();

    Future<?> verifyAuthenticated();

    Future<UserClaims> getUserClaims();

    Future<Void> logout();

}
