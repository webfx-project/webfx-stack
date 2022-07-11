package dev.webfx.stack.framework.shared.services.authn.spi;

import dev.webfx.stack.async.Future;

/**
 * @author Bruno Salmon
 */
public interface AuthenticationServiceProvider {

    Future<?> authenticate(Object userCredentials);

}
