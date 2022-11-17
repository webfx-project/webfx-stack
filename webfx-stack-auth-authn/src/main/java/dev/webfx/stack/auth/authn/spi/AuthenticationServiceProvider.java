package dev.webfx.stack.auth.authn.spi;

import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface AuthenticationServiceProvider {

    Future<?> authenticate(Object userCredentials);

}
