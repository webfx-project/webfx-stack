package dev.webfx.stack.authn.spi;

import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface AuthenticationServiceProvider {

    Future<?> authenticate(Object userCredentials);

}
