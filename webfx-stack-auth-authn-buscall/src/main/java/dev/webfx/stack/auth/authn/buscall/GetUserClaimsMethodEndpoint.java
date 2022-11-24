package dev.webfx.stack.auth.authn.buscall;

import dev.webfx.stack.auth.authn.AuthenticationService;
import dev.webfx.stack.auth.authn.UserClaims;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class GetUserClaimsMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, UserClaims> {

    public GetUserClaimsMethodEndpoint() {
        super(AuthenticationServiceBusAddress.GET_USER_CLAIMS_METHOD_ADDRESS, AuthenticationService::getUserClaims);
    }
}