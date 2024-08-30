package dev.webfx.stack.authn.buscall;

import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class UpdateCredentialsMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, Object> {

    public UpdateCredentialsMethodEndpoint() {
        super(AuthenticationServiceBusAddress.UPDATE_CREDENTIALS_METHOD_ADDRESS, updateCredentialsArgument -> AuthenticationService.updateCredentials(updateCredentialsArgument).map(x -> (Object) x));
    }
}
