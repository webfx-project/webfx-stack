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

    Future<Void> logout();

}
