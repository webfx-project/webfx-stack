package dev.webfx.stack.authn.buscall;

import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class AuthenticateMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, Object> {

    public AuthenticateMethodEndpoint() {
        super(AuthenticationServiceBusAddress.AUTHENTICATE_METHOD_ADDRESS, userCredentials -> AuthenticationService.authenticate(userCredentials).map(x -> (Object) x));
        // We take the opportunity of this call to also instantiate the authentication provider right now, i.e. even
        // before the first client bus call. This is important on server side, because this will instantiate the
        // authentication portal (as ServerAuthenticationPortalProvider is the Authentication provider on server side),
        // which will instantiate (if you look at the ServerAuthenticationPortalProvider() constructor) the registered
        // authentication gateways (such as Google, Facebook, etc...), and call their boot() method, which may do some
        // initialisation (ex: fetching Facebook application access token).
        // If we don't do it right now on server start, it will be done later on first client login request, but this
        // can be too late for all possible users already engaged in the login process just before the server restart.
        AuthenticationService.getProvider();
    }
}
