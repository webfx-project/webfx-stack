package dev.webfx.stack.authn.buscall;

import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * Ends every session of the caller except the one making the call, and answers how many.
 *
 * <p>The argument is ignored, deliberately: who is being signed out is read server-side from the token
 * this call carried, never from anything sent with it. A target parameter would make this "sign anybody
 * out", which against sequential person ids is everybody.
 *
 * @author Claude Code
 */
public final class RevokeOtherSessionsMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, Integer> {

    public RevokeOtherSessionsMethodEndpoint() {
        super(AuthenticationServiceBusAddress.REVOKE_OTHER_SESSIONS_METHOD_ADDRESS,
            ignoredArgument -> AuthenticationService.revokeOtherSessions());
    }
}
