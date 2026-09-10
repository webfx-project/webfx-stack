package dev.webfx.stack.session.state.server;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.tuples.Pair;
import dev.webfx.stack.authn.logout.server.LogoutPush;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.isolation.IsolatedSession;
import dev.webfx.stack.session.state.LogoutUserId;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.push.server.PushServerService;
import dev.webfx.stack.session.token.IdentityToken;
import dev.webfx.stack.session.token.IdentityTokenPolicy;
import dev.webfx.stack.session.token.PrincipalToken;
import dev.webfx.stack.session.token.SessionLifetime;
import dev.webfx.stack.session.token.SessionTier;
import dev.webfx.stack.session.token.SessionTokenService;
import dev.webfx.stack.session.token.SignedToken;
import dev.webfx.stack.session.state.RestrictedPrincipalRegistry;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    // client on every deploy). This map memorizes the in-flight check per server session so messages 2..N
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
        // The identity is settled BEFORE anything below reads the userId, and it is the one step here that can
        // need a database round trip — see applyIdentityToken for when, which is rarely.
        return applyIdentityToken(clientState, serverSession)
            .compose(ignored -> syncFixedServerSessionFromCheckedIncomingClientState(serverSession, clientState, forceStore));
    }

    private static Future<IsolatedSession> syncFixedServerSessionFromCheckedIncomingClientState(IsolatedSession serverSession, Object clientState, boolean forceStore) {
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
                // We are now ready for the push — unless there is nobody to push to. A push is addressed by
                // runId, and with none the push service does computeIfAbsent(null, ..) on a ConcurrentHashMap
                // and throws, which the job above swallows into a log line: a silent failure on the identity
                // path, which is where they cost the most. The sibling anonymous branch further up already
                // guards this way; both paths now agree.
                if (StateAccessor.getRunId(clientState) == null) {
                    Console.log("🛡 Cannot push a login or logout to a client with no runId (session id = "
                                + serverSession.id() + ")");
                    return future;
                }
                ThreadLocalStateHolder.runWithState(clientState, () -> { // we specify which state to use for the push
                    // Special case: invalid user => we force a logout
                    if (LogoutUserId.isLogoutUserId(ThreadLocalStateHolder.getUserId()))
                        LogoutPush.pushLogoutMessageToClient();
                    // The authorizations are pushed in BOTH cases, and the logout case is the one that used to
                    // be missed. It relied on syncOutgoingState firing the authorizer when it saw the userId
                    // change — but syncFixedServerSessionFromIncomingClientState, three lines above, has
                    // ALREADY written LOGOUT_USER_ID into the session. So by the time the logout push goes out,
                    // changeUserId reports no change, nothing fires, and the client is told it is logged out
                    // without ever being told what a logged-out caller may do.
                    //
                    // The client then waits for rules that never come. In the front office that is an
                    // indefinite spinner on a protected page, because the layout blocks on isLoaded before it
                    // will render the login form; in the back office, where rules from the previous session are
                    // still loaded, the route guard evaluates those instead and renders "access restricted".
                    // Both clear on a reload, because a fresh connection takes the public-authorization path
                    // further up — which is exactly what makes this look like a client bug and is not one.
                    //
                    // A user-initiated logout was never affected: there the incoming message still carries a
                    // valid identity, so the session is updated by the OUTGOING state and the change is real.
                    // That is why this survived until identity tokens became required, which turns "an invalid
                    // token" from something only a forger produces into the routine path for any expired one.
                    //
                    // Note: that push must not contain the userId, otherwise this will create a loop (see
                    // OUTGOING STATE) — userIdAuthorizer.apply(null) pushes the rules alone.
                    if (userIdAuthorizer != null)
                        userIdAuthorizer.apply(null);
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
     * Replaces the caller's CLAIMED identity with the one this server can prove, when it presented a token —
     * and keeps that proof alive by exchanging the token as it nears the end of its life.
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
     *   <li><b>Ours, but past its signed deadline</b> — the signature holds and the session has run out of
     *       idle time. Refusing this was a bug with teeth. Tokens carried a fixed 12-hour life and nothing
     *       renewed them, so every session was being ended mid-use — mid-stream, mid-booking, mid-payment —
     *       twelve hours after login, on a server whose whole premise was that it still accepts bare claims
     *       and therefore changes nothing. The flip governs a MISSING token; it never governed a stale one,
     *       so merely installing a signing key switched on a session cap nobody chose and nobody could see.
     *       While the flip is off the claim now stands, exactly as it does for a client that sends no token
     *       at all — the same trust as before, not more.</li>
     *   <li><b>Not ours</b> — forged, altered, malformed, or signed with a key this server no longer
     *       accepts. The claim is NOT honoured as a fallback, in either mode: a caller holding a token that
     *       does not verify has something wrong with it, and quietly dropping back to the weaker path would
     *       let anyone defeat this check by sending rubbish. Treated as logged out, and logged, because it
     *       is the one case here that should be visible to a human.</li>
     * </ul>
     *
     * <h3>Renewal, and why it belongs exactly here</h3>
     *
     * <p>A session must be renewed on activity the SERVER observed, never on activity a client asserts. This
     * method runs on every message a client SENDS OR PUBLISHES, so "server-observed activity" needs no new
     * signal and no new trust: it is simply being called. That is what makes the front office's
     * sixty-second media heartbeat count as presence for free, and it is the difference between a member
     * listening to a recording at 2am staying signed in and being ejected mid-teaching.
     *
     * <p>Note the boundary precisely, because it is doing work: the Vert.x bridge routes only SEND, PUBLISH
     * and RECEIVE through the state sync. A protocol PING — which a client emits every thirty seconds for
     * as long as its socket is open — is handled in the bridge's own branch and never reaches here, so it
     * renews nothing. The client's ping does carry a state header, and the server simply does not read it.
     * That is the right way round: a keepalive proves a socket is open, not that anybody is there, and if
     * it renewed the session then a tab left open on a locked laptop would keep itself signed in forever
     * and the idle window would stop meaning anything at all. REGISTER frames are outside the sync for the
     * same reason (and already were — see the note in the React client's connect handler).
     *
     * <p>Two speeds, because only one of the two cases can afford to wait:
     *
     * <ul>
     *   <li><b>Due, but still usable</b> — the message proceeds immediately on the token it has, and the
     *       exchange runs behind it. Nothing waits on the database.</li>
     *   <li><b>Past its access window</b> — there is no usable proof until the exchange completes, so this
     *       message waits for it. That is the only path here that adds a round trip, and it is reached
     *       roughly once per session per twenty minutes, or once when a client comes back after a break.</li>
     * </ul>
     *
     * <p>Note what a failed exchange does NOT do: a store that cannot answer leaves the session exactly as it
     * was, on the token it already holds. Treating "the database did not answer" as "this session is over"
     * would make any blip on that table a mass logout — the same error as recovering a failed identity check
     * into a logged-out user, in the one place it would hurt most.
     *
     * <p>Cheaper than the database check it will eventually replace, which is what allows it to run on every
     * message rather than only on a login transition — the reason the skip conditions below exist at all.
     */
    private static Future<Void> applyIdentityToken(Object clientState, IsolatedSession serverSession) {
        String serverSessionId = serverSession.id();
        String token = StateAccessor.getUserToken(clientState);
        if (token == null || token.isEmpty()) {
            pendingRenewedTokens.remove(serverSessionId); // nothing to hand a client that is no longer holding one
            boolean claimsRealIdentity = !LogoutUserId.isLogoutUserIdOrNull(StateAccessor.getUserId(clientState));
            if (IdentityTokenPolicy.refusesUntokenedClaim(claimsRealIdentity))
                refuseUntokenedClaim(clientState);
            return Future.succeededFuture();
        }
        long nowMillis = System.currentTimeMillis();
        IdentityToken identity = PrincipalToken.verify(token, nowMillis);
        if (identity == null) {
            if (IdentityTokenPolicy.isTokenRequired() || !SignedToken.isAuthenticButExpired(token, nowMillis)) {
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
                noteExpiredIdentityToken(serverSessionId);
            }
            return Future.succeededFuture();
        }
        StateAccessor.setUserId(clientState, identity.principal());
        // Recorded for whatever needs to act on the SESSION rather than on this message — logout, which has to
        // end the family and not merely the caller's copy of its token. Set from the verified token only, so
        // no caller can name a family it does not hold.
        StateAccessor.setSessionFamilyId(clientState, identity.familyId());
        // This session's successor token has already been minted and the client is still presenting the
        // one it replaces — so it simply has not received it yet, and it will ride this message's reply.
        // Asking the store again here would be asking it about a generation THIS SERVER retired, which it
        // cannot tell from a copy in someone else's hands: the check would report a theft that never
        // happened and end a member's session for it. See pendingRenewedTokens.
        if (awaitingDelivery(serverSessionId, token, nowMillis))
            return Future.succeededFuture();
        if (identity.isWithinAccessWindow(nowMillis)) {
            if (identity.isRenewalDue(nowMillis))
                renewIdentityToken(identity, token, clientState, serverSession, nowMillis); // behind this message, not in front of it
            return Future.succeededFuture();
        }
        // Authentic, inside the session's idle window, but past the window in which it may be USED. The
        // message has no proven identity until the exchange happens, so this one waits.
        return renewIdentityToken(identity, token, clientState, serverSession, nowMillis)
            .map(renewal -> {
                if (renewal.outcome() == SessionTokenService.TokenRenewal.Outcome.ENDED)
                    StateAccessor.setUserId(clientState, LogoutUserId.LOGOUT_USER_ID);
                // Otherwise RENEWED, or KEEP because the store could not answer. KEEP honours the identity on
                // a token whose access window has lapsed, which is deliberate: the signature still holds, and
                // the only thing that could not be established is whether the generation was retired.
                // Refusing here would turn a database outage into a sign-out for everyone at once.
                return null;
            });
    }

    /**
     * In-flight exchanges, keyed by session family.
     *
     * <p>A client sends several messages at once — a reconnection burst, a page that loads four things —
     * and every one of them sees the same token in the same state. Without this each would start its own
     * exchange, and all but one would then present a generation the winner had just retired, which is
     * indistinguishable from the theft signal this exists to raise. Concurrent by type rather than the plain
     * map used for user-id checks above, because a family is shared across connections: two browser tabs are
     * two sockets, and nothing guarantees they land on the same event loop.
     */
    private static final Map<String, Future<SessionTokenService.TokenRenewal>> pendingRenewals = new ConcurrentHashMap<>();

    private static Future<SessionTokenService.TokenRenewal> renewIdentityToken(IdentityToken identity, String presentedToken, Object clientState, IsolatedSession serverSession, long nowMillis) {
        // A token with no family has nothing shared to key on, so the server session stands in for one. It is
        // the right grain: such a token is about to be upgraded, and an upgrade is per client, not per family.
        String key = identity.familyId() != null ? "family:" + identity.familyId() : "session:" + serverSession.id();
        // Claimed with putIfAbsent rather than get-then-put, so two event loops racing on the same family
        // cannot both start an exchange. The loser of that race would present a generation the winner had
        // just retired — the theft signal, raised against the same client. The claim is a bare promise, so
        // it is placed BEFORE any work begins; starting first and registering afterwards would leave the
        // window open in exactly the case this exists to close.
        Promise<SessionTokenService.TokenRenewal> promise = Promise.promise();
        Future<SessionTokenService.TokenRenewal> claim = promise.future();
        Future<SessionTokenService.TokenRenewal> inFlight = pendingRenewals.putIfAbsent(key, claim);
        if (inFlight != null)
            return inFlight;
        String runId = StateAccessor.getRunId(clientState);
        if (runId == null)
            runId = SessionAccessor.getRunId(serverSession);
        String pushRunId = runId;
        String serverSessionId = serverSession.id();
        SessionTokenService.renew(identity, legacyTierHint(identity, clientState, serverSession), nowMillis)
            .onComplete(ar -> {
                pendingRenewals.remove(key, claim);
                if (ar.succeeded()) {
                    deliverRenewal(ar.result(), presentedToken, serverSessionId, pushRunId);
                    promise.complete(ar.result());
                } else {
                    promise.fail(ar.cause());
                }
            });
        return claim;
    }

    /**
     * Which lifetime tier to adopt for a token minted before tiers existed.
     *
     * <p>Only ever consulted for those: a tier that arrived inside the signature is a fact, and re-deriving
     * it from live client state on every message would hand the choice back to the caller. This is the one
     * moment there is nothing signed to read, and the alternative — refusing every token minted before this
     * change — would sign out everyone holding one on the day it deploys.
     */
    private static SessionTier legacyTierHint(IdentityToken identity, Object clientState, IsolatedSession serverSession) {
        if (RestrictedPrincipalRegistry.isUserRestricted(identity.principal()))
            return SessionTier.SUPPORT_VIEW;
        Boolean backoffice = StateAccessor.getBackoffice(clientState);
        if (backoffice == null)
            backoffice = SessionAccessor.isBackoffice(serverSession);
        return Boolean.TRUE.equals(backoffice) ? SessionTier.BACK_OFFICE : SessionTier.FRONT_OFFICE;
    }

    /**
     * A token a renewal minted, and the one it replaces, held until the client is seen using the new one.
     *
     * <p><b>This is what stops rotation eating its own users.</b> A renewal retires a generation the instant
     * it mints its successor, so from that moment the client is holding something the store would call
     * retired — and "retired token presented" is precisely the signal that ends a session family. Everything
     * therefore turns on the successor actually reaching the client, and there is no moment at which that is
     * guaranteed: a renewal that finishes after its message's reply has gone waits for the next one, and a
     * connection can drop in between. Remembering the pair closes that gap without weakening the signal:
     * while the client is still presenting the token we replaced, we know why, so we say the new one again
     * instead of asking the store a question we already know it will answer wrongly.
     *
     * <p>Kept until the client is observed on a DIFFERENT token, not until it is sent once — being sent is
     * not being received, and this whole entry exists because those two are not the same thing.
     *
     * <p>It is per server session, so it protects a client from its own missed delivery. It does not help a
     * SECOND browser tab, which has its own socket and its own session while sharing one stored token: that
     * one is covered by the client propagating a renewed token between tabs, and behind that by the store's
     * short grace. Both are stated where they live.
     *
     * <p>Bounded, evicting the oldest, because an entry is removed when its client speaks again and a client
     * may never do so. Losing one costs an extra exchange, not a session.
     */
    private record PendingToken(String replacedToken, String newToken, long mintedAtMillis) {}

    /**
     * How long a successor may go unclaimed before its predecessor stops being excused.
     *
     * <p>This entry suppresses the generation check, so without a bound it would also suppress the ACCESS
     * WINDOW: a holder that simply never adopts the successor would be honoured until the session's signed
     * deadline — ninety days for a member — which is the opposite of "a stolen token is good for thirty
     * minutes". One access window is long enough to cover every honest reason a client has not caught up
     * (a reply lost with the connection, a laptop closed between the renewal and the next message) and
     * short enough that ignoring the successor buys an extra half hour rather than a season.
     *
     * <p>What it costs: a client that was disconnected for LONGER than this at the exact moment its
     * renewal reply was lost comes back on the retired token, is judged by the store, and has its family
     * ended. That is a re-login, it is logged, and it is the direction to be wrong in.
     *
     * <p>Deliberately NOT shortened by a development build's lifetime scale, for the reason the reuse grace
     * is not: it measures how long a client may take to RECEIVE something, and a connection drop or a
     * closed laptop does not get shorter because sessions do. Scaled, it fell below both the twenty-second
     * push ping that delivers a pending token and the store's one-minute reuse grace, so an ordinary
     * minute offline ended the family as a theft — on a developer's machine and nowhere else. The cost of
     * leaving it whole is confined to that machine too: there, a client that ignores its own successor is
     * excused for longer than the scaled access window.
     */
    private static final long PENDING_DELIVERY_GRACE_MILLIS = SessionLifetime.ACCESS_WINDOW_BASE_MILLIS;

    private static final int PENDING_TOKEN_CAP = 20_000;
    private static final Map<String, PendingToken> pendingRenewedTokens = Collections.synchronizedMap(
        new LinkedHashMap<>(256, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, PendingToken> eldest) {
                return size() > PENDING_TOKEN_CAP;
            }
        });

    /**
     * True when this session has a successor token the client has not picked up yet.
     *
     * <p>Also the place the entry is retired: a client presenting anything OTHER than the token we replaced
     * has moved on, so there is nothing left to deliver.
     */
    private static boolean awaitingDelivery(String serverSessionId, String presentedToken, long nowMillis) {
        PendingToken pending = pendingRenewedTokens.get(serverSessionId);
        if (pending == null)
            return false;
        if (!pending.replacedToken().equals(presentedToken)) {
            pendingRenewedTokens.remove(serverSessionId, pending);
            return false;
        }
        if (nowMillis - pending.mintedAtMillis() <= PENDING_DELIVERY_GRACE_MILLIS)
            return true;
        // Long enough. Stop excusing the old token and let the store say what it thinks of it — which,
        // for a client that has had every message since offering it the successor, is unlikely to be kind.
        Console.log("🛡 A renewed identity token went unclaimed for a whole access window; judging the"
                    + " token still being presented on its own merits");
        pendingRenewedTokens.remove(serverSessionId, pending);
        return false;
    }

    private static void deliverRenewal(SessionTokenService.TokenRenewal renewal, String replacedToken, String serverSessionId, String runId) {
        switch (renewal.outcome()) {
            case RENEWED -> pendingRenewedTokens.put(serverSessionId,
                new PendingToken(replacedToken, renewal.token(), System.currentTimeMillis()));
            case ENDED -> {
                pendingRenewedTokens.remove(serverSessionId);
                // Pushed rather than left for the client's next message, because this is the one outcome where
                // the delay matters. Be precise about what it achieves, though: a COOPERATING client acts on
                // the push and stops immediately, and every holder is refused at its next renewal because the
                // family is revoked in the store — but a token already issued cannot be recalled, so a holder
                // that ignores the push keeps working until its access window runs out (at most thirty
                // minutes). Bounding that further would mean consulting shared state on every message, which
                // is the cost this whole design exists to avoid. Pushed rather than routed through LogoutPush
                // because that reads the runId from the thread, and this runs after an async hop where the
                // thread no longer holds the caller's state.
                if (runId != null)
                    PushServerService.pushState(StateAccessor.createUserIdState(LogoutUserId.LOGOUT_USER_ID), runId)
                        .onFailure(e -> Console.log("⚠️ Could not push the end of a session to its client: " + e));
            }
            case KEEP -> {} // nothing was decided, so nothing is said; the client keeps working and retries
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

    /**
     * Whether a token minted by a renewal may ride on this particular outgoing message.
     *
     * <p>Extracted so the rule can be exercised without a live session and bus — see RenewalDeliveryCheck.
     * Both "no" answers are the load-bearing part of the delivery mechanism rather than edge cases; the
     * reasoning is at the call site.
     *
     * @param outgoingUserId    the identity this message is telling the client it has, if any
     * @param outgoingUserToken the token this message already carries, if any
     * @param sessionUserId     the identity the SERVER SESSION holds right now
     */
    static boolean mayDeliverPendingToken(Object outgoingUserId, String outgoingUserToken, Object sessionUserId) {
        if (outgoingUserId != null || outgoingUserToken != null)
            return false; // this message is settling who the client is; nothing left over may override it
        return !LogoutUserId.isLogoutUserIdOrNull(sessionUserId); // the session is over, or never began
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
        // outgoingState.userToken <= a token a renewal minted for this session ? ON EVERY MESSAGE UNTIL IT LANDS,
        // EXCEPT ON A MESSAGE THAT IS ITSELF SETTLING WHO THE CLIENT IS.
        //
        // Every incoming message produces an outgoing one, so a token minted while handling a message rides its
        // own reply home. Repeated rather than sent once because being sent is not being received: the entry is
        // cleared when the client is next SEEN on the new token (see awaitingDelivery), which is the only
        // evidence of delivery this side ever gets.
        //
        // The exception is not a refinement, it is the whole safety of the mechanism. Two messages must never
        // carry a leftover token:
        //
        //   * A LOGOUT. It arrives as an outgoing state whose userId is LOGOUT_USER_ID, and stapling a live
        //     token to it hands the client a working proof of the identity it was just told to forget — a
        //     logout that does not log out. On a shared device the next person reloads the page and is signed
        //     in as the member who thought they had signed out. The session's own userId is consulted rather
        //     than only this message's, because the reply and the authorization push that FOLLOW a logout carry
        //     no userId of their own and would otherwise deliver the same token a moment later.
        //   * A LOGIN. It already carries the token the gateway minted after checking a credential, and
        //     setUserToken overrides — so a renewal pending for the PREVIOUS occupant of this session would
        //     replace it, and someone who had just typed their own password would be handed the account of
        //     whoever used the device before them.
        //
        // In both cases the entry is dropped rather than merely skipped: whatever the client should be holding
        // is being established right here, so nothing left over from before is worth delivering afterwards.
        PendingToken pendingToken = pendingRenewedTokens.get(serverSession.id());
        if (pendingToken != null) {
            // The session's userId is read AFTER changeUserId above, so a logout push has already put
            // LOGOUT_USER_ID there and every message that follows it sees the session as ended.
            if (mayDeliverPendingToken(StateAccessor.getUserId(outgoingState),
                    StateAccessor.getUserToken(outgoingState), SessionAccessor.getUserId(serverSession)))
                outgoingState = StateAccessor.setUserToken(outgoingState, pendingToken.newToken());
            else
                pendingRenewedTokens.remove(serverSession.id(), pendingToken);
        }
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
