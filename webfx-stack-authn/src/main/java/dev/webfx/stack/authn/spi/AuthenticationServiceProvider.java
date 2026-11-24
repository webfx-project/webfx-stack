package dev.webfx.stack.authn.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.UserClaims;

/**
 * @author Bruno Salmon
 */
public interface AuthenticationServiceProvider {

    Future<?> authenticate(Object userCredentials);

    Future<?> verifyAuthenticated();

    Future<UserClaims> getUserClaims();

    Future<?> updateCredentials(Object updateCredentialsArgument);

    Future<Void> logout();

    /**
     * Ends every session of the caller except this one. Takes no target: the server reads who is asking
     * from the token it verified, because a parameter here would let anyone sign anyone else out.
     */
    Future<Integer> revokeOtherSessions();

}
