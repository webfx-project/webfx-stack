package dev.webfx.stack.db.query.cli;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.AuthenticateWithUsernamePasswordCredentials;
import dev.webfx.stack.authn.AuthenticateWithVerificationCodeCredentials;
import dev.webfx.stack.authn.SendMagicLinkCredentials;
import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.authn.buscall.AuthenticationServiceBusAddress;
import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.com.bus.call.BusCallService;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.client.ClientSideStateSession;

/**
 * The caller a run probes as: signed in with a password the operator typed, or restored from the cache, and in
 * both cases only usable once the audience it actually got has been confirmed.
 *
 * <h3>Why the login goes through {@code BusCallService} and the probes do not</h3>
 *
 * <p>{@code AuthenticationService.authenticate} sends the same bus call this does, to the same address — it
 * simply does not let a caller attach a state to the message, and the state is where the {@code backoffice}
 * flag rides. That flag is what the server reads at login to decide the audience it signs into the token, and
 * a CLI's flag cannot come from the usual place: {@code ClientSideStateSession} reads it once, statically, from
 * {@code Meta.getBackoffice()}, which is derived from the executable module's NAME. One executable therefore
 * has one audience for its whole life, and {@code --audience} would be a lie.
 *
 * <p>So the login — and only the login — is sent with explicit delivery options. The outgoing syncer sets the
 * flag with {@code override=false}, so a value already present is left alone: ours wins by being there first,
 * rather than by racing. Everything after the login is unaffected, because from then on the audience comes
 * from the VERIFIED token and no claim can move it.
 *
 * <p>The probes themselves go through {@code QueryService.executeQuery}, untouched — see {@link ClientPath} for
 * why that matters and what would happen if they did not.
 *
 * <h3>The confirmation, which is not optional</h3>
 *
 * <p>Asking for an audience is not getting one. {@link #confirmAudience} reads the audience back off the token
 * the server minted and refuses the run when it is not the one requested or cannot be read at all. A run that
 * skipped this would test one arm and report the other, which is the failure this tool was commissioned
 * after.
 *
 * @author Claude Code
 */
public final class CallerSession {

    private final String username;
    private final Audience requestedAudience;

    private CallerSession(String username, Audience requestedAudience) {
        this.username = username;
        this.requestedAudience = requestedAudience;
    }

    public String username() {
        return username;
    }

    public Audience requestedAudience() {
        return requestedAudience;
    }

    /** The token the server minted for this session, or null before one has arrived. Never printed. */
    public String token() {
        return ClientSideStateSession.getInstance().getUserToken();
    }

    /** The caller the client session believes it is — a principal object, whose {@code toString} is safe. */
    public Object userId() {
        return ClientSideStateSession.getInstance().getUserId();
    }

    /**
     * Signs in with a password, asking for {@code audience}.
     *
     * <p>The password is taken as a {@code char[]} and cleared by the caller: a {@code String} would sit in the
     * heap until a garbage collection that may not come before a crash dump does. It is wrapped in the
     * credentials record for exactly as long as the call takes.
     */
    public static CallerSession signIn(String username, char[] password, Audience audience, long timeoutMillis) {
        Object state = StateAccessor.setBackoffice(StateAccessor.createEmptyState(), audience.backofficeFlag());
        DeliveryOptions options = new DeliveryOptions().setState(state);
        Future<Object> call = BusCallService.call(
            AuthenticationServiceBusAddress.AUTHENTICATE_METHOD_ADDRESS,
            new AuthenticateWithUsernamePasswordCredentials(username, new String(password)),
            options);
        ClientRuntime.await(call, timeoutMillis, "Signing in as " + username);
        return new CallerSession(username, audience);
    }

    /**
     * Presents a cached token and asks the server whether it still stands.
     *
     * <p>The verification round trip is the whole point: a cached token can be past its access window, revoked,
     * or minted by a server that has since rotated its signing key, and all three come back as "no identity"
     * rather than as an error on the probe. Finding out here means the operator is told to sign in again,
     * instead of a probe reporting zero rows and a rehearsal recording it.
     */
    /**
     * Asks the server to send this address a verification code, for a sign-in with no password.
     *
     * <h3>Why a code and not a link</h3>
     *
     * <p>{@code verificationCodeOnly} is passed true, and the flag exists for exactly this case: its own
     * comment says it prevents "losing the application context (code entered within the same page)". A
     * terminal has no page to come back to, so a clickable link would open a browser that signs ITSELF in and
     * leaves this process where it was. A code can be carried to where the session is wanted.
     *
     * <h3>What the server does with it, and what it will not do</h3>
     *
     * <p>The mailbox is not a second factor. For an account that must present one in the back office, a
     * redemption opens a RECOVERY-ONLY session — the back-office tier with the FRONT-OFFICE audience — which
     * every back-office guard refuses. That is deliberate upstream, and this tool needs no special handling
     * for it: {@link #confirmAudience()} reads the arm back off the token and refuses a run that asked for
     * {@code back-office}, rather than probing with a session that cannot answer back-office questions.
     *
     * <p>So this is a password-free sign-in for accounts with no factor requirement, and an honest refusal
     * for the rest. It is not a way around a second factor, and must not be made into one.
     */
    public static void sendVerificationCode(String email, ServerEndpoint endpoint, Audience audience,
                                            long timeoutMillis) {
        Object state = StateAccessor.setBackoffice(StateAccessor.createEmptyState(), audience.backofficeFlag());
        DeliveryOptions options = new DeliveryOptions().setState(state);
        Future<Object> call = BusCallService.call(
            AuthenticationServiceBusAddress.AUTHENTICATE_METHOD_ADDRESS,
            // requestedPath "/" and a null language and context: the first is where a link would land and is
            // moot for a code, and the last two let the server pick its own default mailbox and template. A
            // context is Modality's own type, which this module deliberately does not know.
            new SendMagicLinkCredentials(email, endpoint.httpOrigin(), "/", null, true, null),
            options);
        ClientRuntime.await(call, timeoutMillis, "Requesting a verification code for " + email);
    }

    /**
     * Signs in by redeeming a verification code, which carries no username: the code identifies the pending
     * link server-side, so {@code username} here is only what this run records about itself.
     *
     * <p>The code is a credential that mints a session, so it is handled exactly as the password is — read
     * from a terminal into a {@code char[]} the caller clears, never taken from the command line where a
     * shell would keep it. The {@code String} the protocol requires is the same unavoidable window the
     * password has, and no wider.
     */
    public static CallerSession signInWithVerificationCode(String username, char[] verificationCode,
                                                           Audience audience, long timeoutMillis) {
        Object state = StateAccessor.setBackoffice(StateAccessor.createEmptyState(), audience.backofficeFlag());
        DeliveryOptions options = new DeliveryOptions().setState(state);
        Future<Object> call = BusCallService.call(
            AuthenticationServiceBusAddress.AUTHENTICATE_METHOD_ADDRESS,
            new AuthenticateWithVerificationCodeCredentials(new String(verificationCode)),
            options);
        ClientRuntime.await(call, timeoutMillis, "Redeeming a verification code as " + username);
        return new CallerSession(username, audience);
    }

    public static CallerSession restore(SessionCache.Entry entry, long timeoutMillis) {
        ClientSideStateSession.getInstance().changeUserToken(entry.token(), false);
        ClientRuntime.await(AuthenticationService.verifyAuthenticated(), timeoutMillis,
                            "Checking the cached session for " + entry.username());
        return new CallerSession(entry.username(), entry.audience());
    }

    /**
     * Why this session is not the audience that was asked for, or null when it is.
     *
     * <p>Call it after signing in or restoring, and before any probe.
     */
    public String confirmAudience() {
        return TokenAudience.refusal(token(), requestedAudience);
    }

    /** The audience the server actually minted, or null when the token does not say. */
    public Audience actualAudience() {
        return TokenAudience.of(token());
    }

    /** Ends this session server-side. The cache is the caller's to clear; this is the half the server sees. */
    public void signOut(long timeoutMillis) {
        ClientRuntime.await(AuthenticationService.logout(), timeoutMillis, "Signing out");
    }
}
