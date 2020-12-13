package dev.webfx.framework.shared.services.authn.spi;

import dev.webfx.platform.shared.util.async.Future;

/**
 * @author Bruno Salmon
 */
public interface AuthenticationServiceProvider {

    Future<?> authenticate(Object userCredentials);

}
