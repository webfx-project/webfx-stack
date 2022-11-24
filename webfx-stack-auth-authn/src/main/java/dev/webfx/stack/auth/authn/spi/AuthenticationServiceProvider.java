package dev.webfx.stack.auth.authn.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.auth.authn.UserClaims;

/**
 * @author Bruno Salmon
 */
public interface AuthenticationServiceProvider {

    Future<?> authenticate(Object userCredentials);

    Future<?> verifyAuthenticated(Object userId);

    Future<UserClaims> getUserClaims(Object userId);

}
