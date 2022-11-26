package dev.webfx.stack.authn.buscall;

import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class LogoutMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, Void> {

    public LogoutMethodEndpoint() {
        super(AuthenticationServiceBusAddress.LOGOUT_METHOD_ADDRESS, userId -> AuthenticationService.logout());
    }
}
