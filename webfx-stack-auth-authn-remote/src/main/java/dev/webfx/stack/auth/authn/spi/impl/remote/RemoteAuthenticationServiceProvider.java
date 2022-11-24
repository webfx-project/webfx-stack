package dev.webfx.stack.auth.authn.spi.impl.remote;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.auth.authn.UserClaims;
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

    @Override
    public Future<?> verifyAuthenticated(Object userId) {
        return BusCallService.call(AuthenticationServiceBusAddress.VERIFY_AUTHENTICATED_METHOD_ADDRESS, userId);
    }

    @Override
    public Future<UserClaims> getUserClaims(Object userId) {
        return BusCallService.call(AuthenticationServiceBusAddress.GET_USER_CLAIMS_METHOD_ADDRESS, userId);
    }
}
