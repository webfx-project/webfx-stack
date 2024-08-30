package dev.webfx.stack.authn.spi.impl.remote;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.buscall.AuthenticationServiceBusAddress;
import dev.webfx.stack.authn.spi.AuthenticationServiceProvider;
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
    public Future<?> verifyAuthenticated() {
        return BusCallService.call(AuthenticationServiceBusAddress.VERIFY_AUTHENTICATED_METHOD_ADDRESS, null);
    }

    @Override
    public Future<UserClaims> getUserClaims() {
        return BusCallService.call(AuthenticationServiceBusAddress.GET_USER_CLAIMS_METHOD_ADDRESS, null);
    }

    @Override
    public Future<?> updateCredentials(Object updateCredentialsArgument) {
        return BusCallService.call(AuthenticationServiceBusAddress.UPDATE_CREDENTIALS_METHOD_ADDRESS, updateCredentialsArgument);
    }

    @Override
    public Future<Void> logout() {
        return BusCallService.call(AuthenticationServiceBusAddress.LOGOUT_METHOD_ADDRESS, null);
    }
}
