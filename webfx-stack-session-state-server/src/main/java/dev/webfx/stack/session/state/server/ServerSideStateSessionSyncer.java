package dev.webfx.stack.session.state.server;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.tuples.Pair;
import dev.webfx.stack.authn.logout.server.LogoutPush;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.isolation.IsolatedSession;
import dev.webfx.stack.session.state.LogoutUserId;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.token.IdentityTokenPolicy;
import dev.webfx.stack.session.token.PrincipalToken;
import dev.webfx.stack.session.token.SignedToken;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Bruno Salmon
 */
public final class ServerSideStateSessionSyncer {

    private final static boolean LOG_STATES = false; // Set to true to log incoming and outgoing states on the server side

    // On (re)connection the client re-sends its full state on EVERY message queued until the first server
    // reply, and the session's userId/runId are only stored once the async userIdChecker completes — so all
    // messages arriving in that window would each re-fire the checker AND the authorizer (a burst of SQL per
    // client on every deploy). This map memoizes the in-flight check per server session so messages 2..N
    // await the single check + authorization push. Single-threaded access (Vert.x event loop) => plain HashMap.
    private record PendingUserIdCheck(Object userId, Future<IsolatedSession> future) {}
    private static final Map<String, PendingUserIdCheck> pendingUserIdChecks = new HashMap<>();

    private static AsyncFunction<Object, Object> userIdChecker;

    public static void setUserIdChecker(AsyncFunction<Object, Object> userIdChecker) {
        ServerSideStateSessionSyncer.userIdChecker = userIdChecker;
    }

    private static AsyncFunction<Void, Void> userIdAuthorizer;

    public static void setUserIdAuthorizer(AsyncFunction<Void, Void> userIdAuthorizer) {
        ServerSideStateSessionSyncer.userIdAuthorizer = userIdAuthorizer;
    }

    // ======================================== INCOMING STATE ON SERVER ========================================
    // Sync method to be used on the server side, when the server receives an incoming state from a client

    public static Future<Pair<IsolatedSession /* final server session */, Object/* final incoming state*/>> syncIncomingState(IsolatedSession serverSession, Object incomingState) {
        String incomingStateCapture = LOG_STATES ? "" + incomingState : null; // capturing state before changes for logs

        Future<IsolatedSession> sessionFuture;

        // serverSession.id <= incomingState.serverSessionId ? ONLY ON NEW SERVER SESSION
        String requestedServerSessionId = StateAccessor.getServerSessionId(incomingState);
        String serverSessionRunId = SessionAccessor.getRunId(serverSession);
        boolean isNewServerSession = serverSessionRunId == null;
        if (requestedServerSessionId != null /*&& isNewServerSession*/ && !Objects.equals(requestedServerSessionId, serverSession.id())) {
            sessionFuture = SessionService.getSessionStore().get(requestedServerSessionId)
                .compose(loadedSession -> {
                    if (loadedSession != null) {
                        serverSession.log("Swapped underlying session (session id = " + serverSession.id() + " -> " + loadedSession.id() + ")");
                        serverSession.setUnderlyingSession(loadedSession);
                    } else {
                        serverSession.log("Unable to load requested session id " + requestedServerSessionId + " -> keeping session id = " + serverSession.id());
                    }
                    return syncFixedServerSessionFromIncomingClientStateWithUserIdCheckFirst(serverSession, incomingState, false);
                });
        } else {
            sessionFuture = syncFixedServerSessionFromIncomingClientStateWithUserIdCheckFirst(serverSession, incomingState, isNewServerSession);
        }

        return sessionFuture.map(finalServerSession -> {
            // Finally, we enrich the incoming state with possible further info coming from the serverSession
            Object finalIncomingState = ServerSideStateSessionSyncer.syncIncomingClientStateFromServerSession(incomingState, finalServerSession);

            if (LOG_STATES)
                Console.log("👉👉 Incoming state: " + incomingStateCapture + " >> " + finalIncomingState);

            return new Pair<>(finalServerSession, finalIncomingState);
        });
    }

    private static Future<IsolatedSession> syncFixedServerSessionFromIncomingClientStateWithUserIdCheckFirst(IsolatedSession serverSession, Object clientState, boolean forceStore) {
        applyIdentityToken(clientState, serverSession.id());
        Object userId = StateAccessor.getUserId(clientState);
        Object sessionUserId = SessionAccessor.getUserId(serverSession);
        // A "public" principal is one that isn't a logged-in user: either no userId at all (never logged in),
        // or the explicit LOGOUT_USER_ID. Note the client keeps re-communicating LOGOUT_USER_ID on every
        // message, INCLUDING on a fresh app start / page reload — so on a logged-out reload the incoming userId
        // is LOGOUT_USER_ID, not null.
        boolean incomingPublic = LogoutUserId.isLogoutUserIdOrNull(userId);
        boolean sessionPublic = LogoutUserId.isLogoutUserIdOrNull(sessionUserId);
        boolean sameConnection = Objects.equals(StateAccessor.getRunId(clientState), SessionAccessor.getRunId(serverSession));
        // Skip the user-identity check when there's no genuine login transition to validate:
        //   - userId == null: the client isn't communicating any userId (the Java client sends it only on change).
        //   - no checker configured.
        //   - same connection and unchanged userId: nothing relevant changed on an existing connection. This
        //     handles clients (e.g. React) that resend all state properties on every request.
        //   - both the incoming principal AND the session are public (logged out / never logged in): the client
        //     is merely re-communicating its logged-out state (e.g. on a page reload), NOT performing a fresh
        //     logout transition — so we must NOT divert it through LogoutPush below. A real logout transition
        //     (session still holds a real user) keeps going to the full-check path so LogoutPush still runs.
        // On a new connection (runId mismatch or new session), a logged-in user still runs the full check to
        // ensure authorizations are refreshed even if the userId hasn't changed.
        if (userId == null || userIdChecker == null
                || (sameConnection && Objects.equals(userId, sessionUserId))
                || (incomingPublic && sessionPublic)) {
            // No user identity to check here. We sync the session as usual...
            Future<IsolatedSession> future = syncFixedServerSessionFromIncomingClientState(serverSession, clientState, forceStore);
            // ...but a freshly connected (or reconnected) public client must ALSO receive its authorizations.
            // The system grants authorizations to the public too (operations flagged 'public'), not only to
            // logged-in users, so those must be pushed even when there's no logged-in userId. We push only when:
            //   - it's a new connection (the client communicates a runId that differs from the one already stored
            //     in the session — on the first message of a connection the session has no runId yet), which
            //     avoids re-pushing on every subsequent message of the same connection; AND
            //   - BOTH the incoming principal and the session are public — so we never clobber a genuinely
            //     logged-in session (e.g. a Java client reconnecting without re-sending its userId) with the
            //     public authorization set. Logged-in users are handled by the full-check path below and the
            //     outgoing-state path.
            if (!sameConnection && userIdAuthorizer != null && incomingPublic && sessionPublic) {
                // The push targets the client by runId, so ensure it's set in the state we run the push with.
                if (StateAccessor.getRunId(clientState) == null)
                    StateAccessor.setRunId(clientState, SessionAccessor.getRunId(serverSession));
                if (StateAccessor.getRunId(clientState) != null) // only push when we know which client to push to
                    ThreadLocalStateHolder.runWithState(clientState, () -> userIdAuthorizer.apply(null));
            }
            return future;
        }
        // Case when the user is set => login or user switch, or logout (LOGOUT_USER_ID)
        // Dedup: if the same check is already in flight for this session (parallel messages of the same
        // (re)connection burst), the later messages just await its outcome — checker, session sync and
        // authorization push all run exactly once.
        String sessionId = serverSession.id();
        PendingUserIdCheck pendingCheck = pendingUserIdChecks.get(sessionId);
        if (pendingCheck != null && Objects.equals(pendingCheck.userId, userId))
            return pendingCheck.future;
        Future<IsolatedSession> checkFuture = ThreadLocalStateHolder.runWithState(clientState, () -> userIdChecker.apply(userId))
            // If the user identity check failed (ex: no such user exception), we log out the user
            .recover(e -> Future.succeededFuture(LogoutUserId.LOGOUT_USER_ID))
            .compose(finalUserId -> {
                // Setting the new user id (should be the same as the passed on if valid, or something like "INVALID" if not)
                Console.log("️🛡 UserIdCheck: userId=" + userId + " => finalUserId = " + finalUserId);
                if (finalUserId == null) // Shouldn't arrive but just in case (the user identity check should raise an exception instead)
                    finalUserId = LogoutUserId.LOGOUT_USER_ID;
                // Memorizing the final user id in the client state
                StateAccessor.setUserId(clientState, finalUserId);
                // We continue with the normal session <-> state sync process
                Future<IsolatedSession> future = syncFixedServerSessionFromIncomingClientState(serverSession, clientState, forceStore);
                // At the same time, we do a push to the client of either the logout userId (if it's a logout), or the
                // new authorizations (if it's a login or user switch). To prepare this push, we need to ensure that the
                // userId is set in the client state (the runId is what identifies which client to push to).
                if (StateAccessor.getRunId(clientState) == null) // // if not, we set it from the server session
                    StateAccessor.setRunId(clientState, SessionAccessor.getRunId(serverSession));
                // We are now ready for the push
                ThreadLocalStateHolder.runWithState(clientState, () -> { // we specify which state to use for the push
                    // Special case: invalid user => we force a logout
                    if (LogoutUserId.isLogoutUserId(ThreadLocalStateHolder.getUserId())) {
                        LogoutPush.pushLogoutMessageToClient(); // This will push a logout userId, and subsequently push the new authorizations (see OUTGOING STATE)
                        // General case: valid user (probably a user switch from the client, or a reconnection)
                    } else if (userIdAuthorizer != null) {
                        // We ask the authorizer to push the new authorizations for that user
                        // Note: that push shouldn't contain the userId, otherwise this will create a loop (see OUTGOING STATE).
                        userIdAuthorizer.apply(null);
                    }
                });
                return future;
            });
        pendingUserIdChecks.put(sessionId, new PendingUserIdCheck(userId, checkFuture));
        checkFuture.onComplete(ar -> {
            PendingUserIdCheck current = pendingUserIdChecks.get(sessionId);
            if (current != null && current.future == checkFuture)
                pendingUserIdChecks.remove(sessionId);
        });
        return checkFuture;
    }

    /**
     * Replaces the caller's CLAIMED identity with the one this server can prove, when it presented a token.
     *
     * <p>Everything below this line reads the userId out of the client's own message. That is the whole of
     * security item 6: the server learns who is calling by reading a field the caller wrote. A token is the
     * server's own signed statement handed back to it, so where one is present it is the better answer and
     * it simply overwrites the claim — the rest of the flow then proceeds unchanged, on an identity that
     * was established rather than asserted.
     *
     * <p>Four cases, and which of them closes anything depends on configuration:
     *
     * <ul>
     *   <li><b>No token</b> — decided by {@link IdentityTokenPolicy}, and this is THE flip. While it is off
     *       the claim stands exactly as before, which is what lets the token be rolled out to clients over
     *       weeks with no coordination and no visible effect. Turned on, a claimed identity with nothing
     *       behind it is refused, and item 6 is closed — for as long as it stays on. Note the asymmetry
     *       worth keeping in mind when reading the rest of this method: every other case here is decided by
     *       cryptography and cannot be got wrong at runtime, while this one is decided by a configuration
     *       value that defaults to the insecure answer.</li>
     *   <li><b>Valid token</b> — its principal wins over whatever the message claimed. A caller presenting
     *       a valid token for one user while claiming to be another is treated as the user the token names.</li>
     *   <li><b>Ours, but expired</b> — the signature holds and the deadline has passed. Refusing this was
     *       a bug with teeth. Tokens carry a fixed 12-hour life and nothing renews them, so every session
     *       was being ended mid-use — mid-stream, mid-booking, mid-payment — twelve hours after login, on a
     *       server whose whole premise was that it still accepts bare claims and therefore changes nothing.
     *       The flip governs a MISSING token; it never governed a stale one, so merely installing a signing
     *       key switched on a session cap nobody chose and nobody could see. While the flip is off the claim
     *       now stands, exactly as it does for a client that sends no token at all — the same trust as
     *       before, not more. Turned on, an expired token is refused like any other unproven claim, which is
     *       precisely why a renewal path has to exist BEFORE the flip does rather than after it.</li>
     *   <li><b>Not ours</b> — forged, altered, malformed, or signed with a key this server no longer
     *       accepts. The claim is NOT honoured as a fallback, in either mode: a caller holding a token that
     *       does not verify has something wrong with it, and quietly dropping back to the weaker path would
     *       let anyone defeat this check by sending rubbish. Treated as logged out, and logged, because it
     *       is the one case here that should be visible to a human.</li>
     * </ul>
     *
     * <p>Cheaper than the database check it will eventually replace, which is what allows it to run on every
     * message rather than only on a login transition — the reason the skip conditions below exist at all.
     */
    private static void applyIdentityToken(Object clientState, String serverSessionId) {
        String token = StateAccessor.getUserToken(clientState);
        if (token == null || token.isEmpty()) {
            boolean claimsRealIdentity = !LogoutUserId.isLogoutUserIdOrNull(StateAccessor.getUserId(clientState));
            if (IdentityTokenPolicy.refusesUntokenedClaim(claimsRealIdentity))
                refuseUntokenedClaim(clientState);
            return;
        }
        long nowMillis = System.currentTimeMillis();
        Object principal = PrincipalToken.verify(token, nowMillis);
        if (principal != null) {
            StateAccessor.setUserId(clientState, principal);
        } else if (IdentityTokenPolicy.isTokenRequired() || !SignedToken.isAuthenticButExpired(token, nowMillis)) {
            // The client is told only that it is logged out, never why — but while the flip is off it can
            // still infer the difference, because an expired token leaves its session alone and this branch
            // does not. That is a MAC-validity oracle, and it is accepted here for two reasons: guessing a
            // valid HMAC-SHA256 is the infeasibility the whole mechanism already rests on, and a caller in
            // this mode can assert any identity it likes with no token at all, so the oracle reveals nothing
            // it could not more easily just do. Do not carry the old "indistinguishable on purpose" claim
            // forward: it stopped being true here. Ordered so the extra signature check is skipped entirely
            // once the flip is on, where both kinds end the same way anyway.
            Console.log("🛡 Identity token presented but not valid — treating as logged out");
            StateAccessor.setUserId(clientState, LogoutUserId.LOGOUT_USER_ID);
        } else {
            // Ours, expired, flip off: leave the state exactly as the claim had it — the same treatment this
            // message would have got carrying no token at all. Deliberately NOT re-minted here: a token
            // minted from a claim is a signed lie (see AuthenticatedState), so renewal belongs where a
            // credential was actually checked, never on the echo path.
            noteExpiredIdentityToken(serverSessionId);
        }
    }

    /**
     * Counts refusals since the last report, so the log measures the tail instead of drowning in it.
     *
     * <p>Deliberately unsynchronised. Concurrent messages can lose a count or produce two lines in the same
     * minute; both are acceptable in a number whose only job is to show an operator whether the tail is
     * shrinking. It is never used to decide anything, which is what makes approximate good enough — do not
     * grow a decision on top of it without making it atomic first.
     */
    private static long untokenedClaimsSinceLastLog;
    private static long untokenedClaimLastLogMillis;
    /** One line a minute: enough to watch a tail shrink, few enough not to fill a disk while it does. */
    private static final long UNTOKENED_CLAIM_LOG_INTERVAL_MILLIS = 60_000;

    /**
     * Applies the flip to a message that claims an identity with nothing backing it.
     *
     * <p>Whether to call this at all is {@link IdentityTokenPolicy#refusesUntokenedClaim(boolean)}, which
     * is where the reasoning about what counts as a claim lives.
     *
     * <p>The rate limit is not cosmetic. This runs on every message from every client, so on the day of the
     * flip an old client in a reload loop generates these continuously, and one line per message would bury
     * the very signal an operator is watching for. Counting and reporting periodically turns the same events
     * into the useful number: how many callers are still on a build that cannot hold a token.
     */
    private static void refuseUntokenedClaim(Object clientState) {
        StateAccessor.setUserId(clientState, LogoutUserId.LOGOUT_USER_ID);
        long now = System.currentTimeMillis();
        untokenedClaimsSinceLastLog++;
        if (now - untokenedClaimLastLogMillis >= UNTOKENED_CLAIM_LOG_INTERVAL_MILLIS) {
            untokenedClaimLastLogMillis = now;
            // The claimed identity is deliberately not logged: it is unproven by definition, so recording it
            // would write attacker-chosen values — plausibly someone else's user id — into the log.
            Console.log("🛡 Refused " + untokenedClaimsSinceLastLog + " identity claim(s) with no token"
                        + " (webfx.stack.session.token.required is on)");
            untokenedClaimsSinceLastLog = 0;
        }
    }

    /**
     * DISTINCT SESSIONS seen with an expired token since the last report — not messages, and the difference
     * is the whole value of the number.
     *
     * <p>The client echoes its token on every single message, so counting messages would report one stale
     * tab as hundreds a minute, would never visibly shrink, and would be wrong in the one direction that
     * matters: this figure exists to tell an operator how many people the flip would sign out, and a number
     * that overstates that by orders of magnitude is worse than no number at all.
     *
     * <p>Concurrency is handled properly here rather than waved off as it is for the counter above, because
     * this one is a collection. A plain {@link HashMap}-backed set written from two event loops at once can
     * corrupt its table and spin a CPU — an outcome far worse than a miscounted log line, and not one to
     * accept for a diagnostic. Capped so a flood of new sessions cannot grow it without bound between
     * reports; at the cap the figure is reported as a floor rather than silently becoming a lie.
     */
    private static final int EXPIRED_TOKEN_SESSIONS_CAP = 10_000;
    private static final Set<String> expiredTokenSessionIds = ConcurrentHashMap.newKeySet();
    private static long expiredTokenLastLogMillis;

    private static void noteExpiredIdentityToken(String serverSessionId) {
        // Bounded, and racy against the cap on purpose: a few entries either side of it change nothing.
        if (expiredTokenSessionIds.size() < EXPIRED_TOKEN_SESSIONS_CAP)
            expiredTokenSessionIds.add(serverSessionId == null ? "" : serverSessionId);
        long now = System.currentTimeMillis();
        if (now - expiredTokenLastLogMillis >= UNTOKENED_CLAIM_LOG_INTERVAL_MILLIS) {
            expiredTokenLastLogMillis = now;
            int sessions = expiredTokenSessionIds.size();
            expiredTokenSessionIds.clear();
            Console.log("🛡 " + sessions + (sessions >= EXPIRED_TOKEN_SESSIONS_CAP ? "+" : "") + " session(s)"
                        + " presented an expired identity token; the claim stands while"
                        + " webfx.stack.session.token.required is off. Each is a session that has outlived"
                        + " its token, and a forced logout on the day that setting is turned on.");
        }
    }

    private static Future<IsolatedSession> syncFixedServerSessionFromIncomingClientState(IsolatedSession serverSession, Object clientState, boolean forceStore) {
        // serverSession.userId <= clientState.userId ? YES IF SET, as this means the client switched user, so we memorise that info in the session
        boolean userIdChanged = SessionAccessor.changeUserId(serverSession, StateAccessor.getUserId(clientState), true);
        // serverSession.runId <= clientState.runId ? YES IF SET, as this means the client is communicating the run id, so we memorise that in the session
        String runId = StateAccessor.getRunId(clientState);
        boolean runIdChanged = SessionAccessor.changeRunId(serverSession, runId, true);
        // serverSession.backoffice <= clientState.backoffice ? YES IF SET, as this means the client is communicating the client type, so we memorise that in the session
        Boolean backoffice = StateAccessor.getBackoffice(clientState);
        boolean backofficeChanged = SessionAccessor.changeBackoffice(serverSession, backoffice, true);
        // serverSession.clientVersion / pwa <= clientState.* ? YES IF SET — invariant connection facts the
        // client sends once at (re)connection; kept in the session (source of truth for the /monitor distributions).
        boolean clientVersionChanged = SessionAccessor.changeClientVersion(serverSession, StateAccessor.getClientVersion(clientState), true);
        boolean pwaChanged = SessionAccessor.changePwa(serverSession, StateAccessor.getPwa(clientState), true);
        boolean clientProfileChanged = SessionAccessor.changeClientProfile(serverSession, StateAccessor.getClientProfile(clientState), true);
        // Since clients communicate the runId on first connection or reconnection, the sessionId must be synced in both cases (on reconnection, the session id may have changed)
        boolean sessionIdSyncedChanged = runId != null && SessionAccessor.changeServerSessionIdSynced(serverSession, false);
        if (userIdChanged || runIdChanged || backofficeChanged || clientVersionChanged || pwaChanged || clientProfileChanged || sessionIdSyncedChanged || forceStore)
            return storeServerSession(serverSession);
        return Future.succeededFuture(serverSession);
    }

    /**
     * The identity the session may lend to a message that did not state one — nothing, once tokens are required.
     *
     * <p>Without this, the flip would refuse a claimed identity while still granting an unclaimed one, and
     * SAYING NOTHING WOULD BE STRONGER THAN CLAIMING SOMETHING: a caller naming a user is turned away, while
     * the same caller omitting the field is handed whatever user the session holds. The claim check alone
     * closes the front door and leaves this open, so both belong to the same flag.
     *
     * <p>Under the flag this suppression costs nothing for a client that proves itself, which is what makes
     * it safe to pair with the other half. By the time this runs, a valid token has already put its principal
     * in the state and a refused claim has already put LOGOUT_USER_ID there — both are "set", so neither is
     * back-filled either way. The only case this changes is the one with nothing behind it at all.
     *
     * <p><b>It is also the half that breaks clients relying on the session to remember them</b> — the WebFX
     * Java clients send their user id only when it changes, so between changes this back-fill is what keeps
     * them logged in. That is not collateral damage but the same fact stated from the other side: a session
     * id is a bearer credential, and "logged in because an earlier message on this session was" is precisely
     * the inference the token exists to replace.
     */
    private static Object backFillableUserId(IsolatedSession serverSession) {
        Object sessionUserId = SessionAccessor.getUserId(serverSession);
        if (IdentityTokenPolicy.isTokenRequired() && !LogoutUserId.isLogoutUserIdOrNull(sessionUserId))
            return null; // unset, so the message stays anonymous rather than inheriting an unproven identity
        return sessionUserId;
    }

    private static Object syncIncomingClientStateFromServerSession(Object clientState, IsolatedSession serverSession) {
        // clientState.serverSessionId <= serverSession.id ? ALWAYS, because this is the server session that is responsible for the session id
        clientState = StateAccessor.setServerSessionId(clientState, serverSession.id(), true);
        // clientState.userId <= serverSession.userId ? YES IF NOT SET, otherwise this means the client switched user, so we keep that info
        clientState = StateAccessor.setUserId(clientState, backFillableUserId(serverSession), false);
        // clientState.runId <= serverSession.runId ? YES IF NOT SET, otherwise this means the client communicates it, so we keep that info
        clientState = StateAccessor.setRunId(clientState, SessionAccessor.getRunId(serverSession), false);
        // clientState.backoffice <= serverSession.backoffice ? YES IF NOT SET, otherwise this means the client communicates it, so we keep that info
        clientState = StateAccessor.setBackoffice(clientState, SessionAccessor.isBackoffice(serverSession), false);
        // clientState.clientVersion <= serverSession.clientVersion ? ALWAYS: the client sends its version once at
        // connection (kept in the session), so copy it into every call's state — lets server code read the caller's
        // client version (e.g. the /monitor Analyze capture tags the plan with the client version that ran it).
        clientState = StateAccessor.setClientVersion(clientState, SessionAccessor.getClientVersion(serverSession));
        return clientState;
    }

    private static Future<IsolatedSession> storeServerSession(IsolatedSession serverSession) {
        return serverSession.store()
            .onFailure(Console::error)
            .map(x -> serverSession);
    }

    // ======================================== OUTGOING STATE ON SERVER ========================================
    // Sync methods to be used on the server side, when the server is about to send a state generated by the server back to the client

    public static Object syncOutgoingState(Object outgoingState, IsolatedSession serverSession) {
        String outgoingStateCapture = LOG_STATES ? "" + outgoingState : null; // capturing state before changes for logs

        // serverSession.id <= outgoingState.serverSessionId ? NEVER (serverSession.id can't be changed at this point)
        // serverSession.userId <= outgoingState.userId ? YES IF SET, as this means the server switched or logged-in user, so we memorize that info in the session
        boolean userIdChanged = SessionAccessor.changeUserId(serverSession, StateAccessor.getUserId(outgoingState), true);
        // serverSession.runId <= outgoingState.runId ? NEVER, because this is info can only come from an incoming outgoingState.
        // outgoingState.sessionId <= serverSession.id ? ONLY if the client doesn't know it already
        boolean sessionIdSyncedChanged = false;
        if (!SessionAccessor.isServerSessionIdSynced(serverSession)) {
            outgoingState = StateAccessor.setServerSessionId(outgoingState, serverSession.id(), true);
            sessionIdSyncedChanged = SessionAccessor.changeServerSessionIdSynced(serverSession, true);
        }
        // outgoingState.serverRunId <= StateAccessor.getServerRunId() ? ALWAYS (non-override), so client can detect server restarts
        outgoingState = StateAccessor.setServerRunId(outgoingState, StateAccessor.getServerRunId(), false);
        // outgoingState.tokenRequired <= the flip's state ? ALWAYS (non-override), so a client holding a token can
        // stop sending a user id this server would only overwrite. Sent every message rather than once because a
        // client that missed it would silently keep claiming, and the whole point is to remove the claim.
        outgoingState = StateAccessor.setTokenRequired(outgoingState, IdentityTokenPolicy.isTokenRequired(), false);
        // outgoingState.userId <= serverSession.userId ? NO, we communicate this info only once to the client (when the server code explicitly sets outgoingState.userId)
        // outgoingState.runId <= serverSession.runId ? NEVER (ERASED), because it's always communicated in the opposite way (client => server)
        // outgoingState.backoffice <= serverSession.backoffice ? NEVER (ERASED), because it's always communicated in the opposite way (client => server)
        outgoingState = StateAccessor.setRunId(outgoingState, null, true);
        // Authorization push management:
        // If a user id is set in that direction, this means the server switched, logged-in or logged-out the user,
        // so we need in all cases to call the authorizer to push the new authorizations to the client.
        // Note: that authorizations push shouldn't contain the user id to avoid a loop here.
        if (userIdChanged || sessionIdSyncedChanged) {
            // Delay the authorization push until AFTER the session is fully stored. This ensures that by the time
            // the client receives the push (and retries getUserDetails), the session store has committed the new
            // userId. Without this ordering, the push can arrive at the client before the store completes,
            // causing a getUserDetails race where the server still sees LOGOUT_USER_ID.
            storeServerSession(serverSession).onComplete(ar -> {
                if (userIdChanged && userIdAuthorizer != null) {
                    // It's important to set the userId and runId in ThreadLocalStateHolder before calling userIdAuthorizer
                    // because it will load the authorizations from userId and push them to the runId client.

                    // Most of the time the runId is in the server session, which matches the associated client. An exception to
                    // that rule is the magic link, where the magic link client and the login client are different. If the magic
                    // link is valid, the authorizations must be pushed to the login client and not to the magic link client
                    // associated with this session. The magic link AuthenticationGatewayProvider indicated this by setting the
                    // login client runId in the server state.

                    // Creating a new state from the session => should contain the userId, runId, and eventually other info (ex: backoffice)
                    Object state = StateAccessor.createStateFromSession(serverSession);
                    ThreadLocalStateHolder.runWithState(state, () -> userIdAuthorizer.apply(null));
                }
            });
        } else if (userIdChanged && userIdAuthorizer != null) {
            Object state = StateAccessor.createStateFromSession(serverSession);
            ThreadLocalStateHolder.runWithState(state, () -> userIdAuthorizer.apply(null));
        }

        if (LOG_STATES)
            Console.log("👈👈 Outgoing state: " + outgoingState + " << " + outgoingStateCapture);

        return outgoingState;
    }

}
