package dev.webfx.stack.auth.authn.buscall;

import dev.webfx.stack.auth.authn.AuthenticationService;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class AuthenticateMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, Object> {

    public AuthenticateMethodEndpoint() {
        super(AuthenticationServiceBusAddress.AUTHENTICATE_METHOD_ADDRESS, userCredentials -> AuthenticationService.authenticate(userCredentials).map(x -> (Object) x));
    }
}
