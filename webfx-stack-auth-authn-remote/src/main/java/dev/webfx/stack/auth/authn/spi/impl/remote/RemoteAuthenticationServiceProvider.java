package dev.webfx.stack.auth.authn.spi.impl.remote;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.auth.authn.buscall.AuthenticationServiceBusAddress;
import dev.webfx.stack.auth.authn.spi.AuthenticationServiceProvider;
import dev.webfx.stack.com.bus.call.BusCallService;

/**
 * @author Bruno Salmon
 */
public class RemoteAuthenticationServiceProvider implements AuthenticationServiceProvider {

    @Override
    public Future<?> authenticate(Object userCredentials) {
        return BusCallService.call(AuthenticationServiceBusAddress.AUTHENTICATE_METHOD_ADDRESS, userCredentials);
    }

}
