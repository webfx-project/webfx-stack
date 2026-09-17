package dev.webfx.stack.authn.buscall;

import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * Exposes {@link AuthenticationService#verifyAuthenticated()} on the bus, so a client can ask the
 * server whether the session it believes it holds is one the server still recognises.
 * <p>
 * Everything behind this has been in place all along — the address constant, the service method,
 * and a verifyAuthenticated() on every gateway. Only this adapter was missing, so the address had
 * nothing listening on it and every client call failed on arrival. The clients could not see that:
 * the bus reported the failure in a form they read as a success, so each one concluded its session
 * was verified and carried on. Sessions have been accepted on the strength of a call that never
 * ran, for as long as the React clients have existed.
 * <p>
 * The result is deliberately dropped. The portal answers with the userId it verified, and the
 * callers ignore it — what they act on is whether the call succeeded. Replying with the principal
 * would put a value on the wire that has to stay encodable for every principal type anyone ever
 * introduces, and a principal without a codec would fail the reply of a verification that had in
 * fact succeeded, logging out the very session it just confirmed. Nothing needs it, so it doesn't
 * travel.
 *
 * @author Bruno Salmon
 */
public final class VerifyAuthenticatedMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, Void> {

    public VerifyAuthenticatedMethodEndpoint() {
        super(AuthenticationServiceBusAddress.VERIFY_AUTHENTICATED_METHOD_ADDRESS,
            ignoredArgument -> AuthenticationService.verifyAuthenticated().map(ignoredUserId -> null));
    }
}
