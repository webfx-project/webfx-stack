package dev.webfx.stack.session.token;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.state.StateAccessor;

/**
 * Builds the state that tells a client who it now is, with the proof attached.
 *
 * <p>Every login in this system ends the same way: the server works out the principal and pushes it to
 * the client as state. Password login does it after checking the password, magic link after redeeming
 * the token. Those are the moments a credential was actually verified, so those are the moments worth
 * signing — which is why this replaces the state construction at exactly those points and nowhere else.
 *
 * <p><b>It does not mint from session state, and that is the whole discipline.</b> Stamping a token
 * wherever a session happens to hold a userId would need no gateway changes and would cover every login
 * for free. It would also sign an assertion nobody established: the session's userId is whatever the
 * client claimed and the existing weak check waved through, so an attacker would receive a properly
 * signed token for an identity they invented. A token is worth exactly what the check behind it was
 * worth.
 *
 * <p>Asynchronous since session families arrived, because opening one writes a row. The alternative —
 * generating the family id here and writing the row in the background — would let a login succeed while
 * its family quietly failed to exist, and the first renewal would then find no row and could not tell
 * that from a family deliberately revoked. Waiting is the honest version.
 *
 * @author Bruno Salmon
 */
public final class AuthenticatedState {

    private static boolean missingKeyAlreadyReported;

    private AuthenticatedState() {}

    /**
     * @param principal         the identity a credential check just established — never one merely claimed
     * @param backofficeSession whether this login is establishing a BACK-OFFICE session, which decides how
     *                          long it may live. <b>Capture it synchronously, at the top of the gateway
     *                          method, exactly as {@code runId} is captured.</b> Every caller reaches this
     *                          point from inside a {@code compose()}, and {@code ThreadLocalStateHolder} is
     *                          restored when the synchronous part of the call returns — so a value read
     *                          here would be "false" for every login ever made, and every staff session
     *                          would quietly get the front office's year-long lifetime.
     */
    public static Future<Object> createFor(Object principal, boolean backofficeSession) {
        Object state = StateAccessor.createUserIdState(principal);
        if (!SignedToken.isConfigured()) {
            reportMissingKeyOnce();
            return Future.succeededFuture(state);
        }
        return SessionTokenService.mintForLogin(principal, backofficeSession)
            .map(token -> token == null ? state : StateAccessor.setUserToken(state, token))
            // A login must not fail because the machinery behind the token did. Nothing verifies tokens
            // strictly yet, so a missing one costs nothing today; that reverses at the flip, and the log
            // line is what makes the reversal visible before it bites.
            .otherwise(e -> {
                Console.log("⚠️ Logging in without an identity token: " + e);
                return state;
            });
    }

    /**
     * Says once per server, not once per login, that no key is configured.
     *
     * <p>Fail-soft deliberately, and only for as long as the migration lasts. Minting throws when no
     * signing key is configured, and this sits on the login path — so a strict version would mean that
     * deploying this change to any server whose key had not been installed yet stopped every user
     * logging in. Nothing verifies tokens today, so a missing token costs nothing today.
     *
     * <p>That reverses when clients start requiring one. At the flip, a server that cannot mint cannot
     * authenticate anyone, and it should refuse to start rather than accept logins it cannot prove —
     * silently issuing identities with no proof is the failure this whole exercise exists to remove.
     */
    private static void reportMissingKeyOnce() {
        if (!missingKeyAlreadyReported) {
            missingKeyAlreadyReported = true;
            Console.log("⚠️ Logging in without an identity token: no signing key configured."
                        + " Harmless until clients require one, and a startup failure after that.");
        }
    }
}
