package dev.webfx.stack.authn.server.gateway.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.UserClaims;

public interface ServerAuthenticationGatewayProvider {

    default void boot() {}

    boolean acceptsUserCredentials(Object userCredentials);

    Future<?> authenticate(Object userCredentials);

    boolean acceptsUserId();

    Future<?> verifyAuthenticated();

    Future<UserClaims> getUserClaims();

    boolean acceptsUpdateCredentialsArgument(Object updateCredentialsArgument);

    Future<?> updateCredentials(Object updateCredentialsArgument);

    Future<Void> logout();

}
