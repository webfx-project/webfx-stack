package dev.webfx.stack.session.token;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;

import java.util.Set;
import dev.webfx.stack.session.state.RestrictedPrincipalRegistry;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Issues session tokens, and exchanges one for its successor.
 *
 * <p>The two things this does are the same act at different moments. {@link #mintForLogin} runs where a
 * credential was actually checked; {@link #renew} carries that check forward without re-doing it. What
 * neither of them will do is manufacture a proof from a claim: renewal takes an {@link IdentityToken},
 * which only exists once a signature has held, and there is deliberately no entry point that takes a
 * user id. Renewing on the echo path would sign an assertion nobody established.
 *
 * @author Claude Code
 */
public final class SessionTokenService {

    private SessionTokenService() {}

    /** What a renewal decided, and the token to hand the client if there is one. */
    public record TokenRenewal(Outcome outcome, String token) {

        public enum Outcome {
            /** A new token was minted; give it to the client. */
            RENEWED,
            /**
             * Nothing could be decided, so nothing changes: the client keeps the token it has and the
             * exchange is tried again on its next message. Reached when the store cannot answer — a
             * database blip must not end a session.
             */
            KEEP,
            /** This session is over: revoked, past its bound, or a retired token turned up in it. */
            ENDED
        }

        static final TokenRenewal KEEP = new TokenRenewal(Outcome.KEEP, null);
        static final TokenRenewal ENDED = new TokenRenewal(Outcome.ENDED, null);

        static TokenRenewal renewed(String token) {
            return token == null ? KEEP : new TokenRenewal(Outcome.RENEWED, token);
        }
    }

    /**
     * Mints the first token of a new session, opening a family to rotate it in.
     *
     * <p>Fail-soft in two different ways, and the difference matters. A missing signing key means no
     * token at all, which is the pre-existing migration behaviour and is handled by the caller. A store
     * that cannot open a family means a token WITHOUT one: the login succeeds, the session slides and is
     * capped as normal, but it cannot be rotated and a stolen copy of it cannot be detected. Refusing the
     * login instead would turn a bookkeeping failure into an outage; running on silently would be worse
     * than either, so it is logged as the reduction in protection that it is.
     */
    public static Future<String> mintForLogin(Object principal, boolean backofficeSession) {
        // The one moment the audience is decided. After this it rides inside the signature and the
        // caller cannot change it, which is the whole of what the claim buys — see SessionAudience.
        return mintForLogin(principal, backofficeSession, SessionAudience.ofBackofficeFlag(backofficeSession),
                            SessionMethod.NONE);
    }

    /**
     * As above, for a login whose audience is a property of the METHOD rather than of the connection.
     *
     * <p>Only the two view-as redeems need this. Each refuses the wrong context before it reaches here,
     * so the flag already implies the right audience today — but it implies it by way of a refusal
     * written somewhere else, and nothing ties the two together. Stating the audience makes the pass's
     * own nature the source, so relaxing that refusal later cannot quietly freeze the wrong one into a
     * signature.
     *
     * @param audience what this session is FOR, where the flag is not the authority on it
     */
    public static Future<String> mintForLogin(Object principal, boolean backofficeSession,
                                              SessionAudience audience) {
        return mintForLogin(principal, backofficeSession, audience, SessionMethod.NONE);
    }

    /**
     * As above, also recording how the login was authenticated.
     *
     * <p>Every gateway should pass its methods: a token that names none is read as saying nothing about
     * them, and anything that insists on a strong factor then has to fall back to what it did before the
     * claim existed. The overloads that omit them exist for the migration, not as a default worth taking.
     *
     * @param methods the methods actually presented — {@link SessionMethod#NONE} where the caller has
     *                nothing to say, never a guess at what is likely
     */
    public static Future<String> mintForLogin(Object principal, boolean backofficeSession,
                                              SessionAudience audience, Set<SessionMethod> methods) {
        if (principal == null || !SignedToken.isConfigured())
            return Future.succeededFuture(null);
        long now = System.currentTimeMillis();
        SessionTier tier = tierForNewSession(principal, backofficeSession);
        long absoluteExpiry = now + SessionLifetime.absoluteLifetimeMillis(tier);
        SessionFamilyStore store = SessionFamilyStoreRegistry.getStore();
        if (store == null)
            return Future.succeededFuture(mintQuietly(principal, null, 0, tier, audience, methods, absoluteExpiry, now));
        return store.open(principal, tier, audience, methods, absoluteExpiry)
            .map(familyId -> mintQuietly(principal, familyId, 0, tier, audience, methods, absoluteExpiry, now, store))
            .otherwise(e -> {
                Console.log("⚠️ Could not open a session family — this session cannot be rotated and a stolen"
                            + " copy of its token will not be detected: " + e);
                return mintQuietly(principal, null, 0, tier, audience, methods, absoluteExpiry, now);
            });
    }

    /**
     * Exchanges a token that is at or past the end of its access window for a fresh one.
     *
     * <p>Renewal is keyed on this being called, and it is called from the one place every client message
     * passes through — so "activity" means traffic the server observed, never anything the client asserts
     * about being active. The front office's media heartbeat is traffic like any other, which is what
     * lets a member listening to a recording at 2am keep their session alive without touching anything.
     *
     * @param tierHint the tier to adopt if the presented token is a legacy one that names none; ignored
     *                 otherwise, because a tier that arrived inside the signature is a fact and one
     *                 derived from live client state is a claim
     */
    public static Future<TokenRenewal> renew(IdentityToken presented, SessionTier tierHint, long nowMillis) {
        if (presented == null || !SignedToken.isConfigured())
            return Future.succeededFuture(TokenRenewal.KEEP);
        SessionTier tier = presented.isLegacy() ? tierHint : presented.tier();
        // Carried forward, NEVER re-read from the connection: re-deriving it here would hand a caller
        // back the per-message flag the claim exists to replace, and a session could change audience by
        // asking.
        //
        // Note that it deliberately does NOT follow tierHint the way the tier above does, and a legacy
        // token is where the difference bites: it carries no audience, so it is read as FRONT_OFFICE and
        // stays there, even on a connection whose hint says back office. Taking the hint would be the
        // escalation this claim exists to close — present an old front-office token on a connection
        // claiming `backoffice`, and walk away with one stamped `bo`, having passed none of the
        // conditions a back-office login imposes. The tier can take the hint safely because every value
        // it might adopt SHORTENS the session; an audience grants.
        SessionAudience audience = presented.audience();
        // Carried forward for the same reason, and with one extra consequence: a session cannot GAIN a
        // method by being renewed. Enrolling a second factor at 11am leaves a session minted at 9am
        // saying what it said. That is the truth about how it was opened, and a step-up meant to take
        // effect at once needs a fresh mint rather than a renewal — see SessionMethod.
        Set<SessionMethod> methods = presented.methods();
        SessionFamilyStore store = SessionFamilyStoreRegistry.getStore();

        // No family yet: a token minted before families existed, or by a server that had no store at the
        // time. Upgrading it opens one and carries the SAME proof forward — the signature already held,
        // so nothing is being asserted here that was not established at login.
        if (presented.familyId() == null) {
            long absoluteExpiry = presented.isLegacy()
                                  ? nowMillis + SessionLifetime.absoluteLifetimeMillis(tier)
                                  : presented.absoluteExpiryMillis();
            if (nowMillis >= absoluteExpiry)
                return Future.succeededFuture(TokenRenewal.ENDED);
            if (store == null)
                return Future.succeededFuture(TokenRenewal.renewed(
                    mintQuietly(presented.principal(), null, 0, tier, audience, methods, absoluteExpiry, nowMillis)));
            return store.open(presented.principal(), tier, audience, methods, absoluteExpiry)
                .map(familyId -> TokenRenewal.renewed(
                    mintQuietly(presented.principal(), familyId, 0, tier, audience, methods, absoluteExpiry, nowMillis)))
                // Same reasoning as at login: a family that cannot be opened costs rotation, not the
                // session. Sliding it forward keeps the user working and the upgrade is retried later.
                .otherwise(e -> {
                    Console.log("⚠️ Could not open a session family while upgrading a token: " + e);
                    return TokenRenewal.renewed(
                        mintQuietly(presented.principal(), null, 0, tier, audience, methods, absoluteExpiry, nowMillis));
                });
        }

        if (store == null) {
            // The token names a family this server cannot look up. Only a misconfiguration reaches here —
            // a deployment that had a store and lost it — and the honest response is to keep the session
            // working while saying, loudly, that the generation is no longer being checked.
            Console.log("⚠️ A token names a session family but no store is registered — rotation and reuse"
                        + " detection are NOT in effect for it");
            return Future.succeededFuture(TokenRenewal.renewed(mintQuietly(presented.principal(), presented.familyId(),
                presented.generation(), tier, audience, methods, presented.absoluteExpiryMillis(), nowMillis)));
        }

        return store.renew(presented.familyId(), presented.generation(), nowMillis)
            .compose(renewal -> switch (renewal.verdict()) {
                case RENEWED, CURRENT -> Future.succeededFuture(TokenRenewal.renewed(
                    mintQuietly(presented.principal(), presented.familyId(), renewal.generation(), tier, audience,
                        methods, renewal.absoluteExpiryMillis(), nowMillis)));
                case REUSE_DETECTED -> {
                    // Two holders, and there is no way to tell from here which one is the member. Killing
                    // the family signs both out; leaving it alive keeps the thief in. Only one of those is
                    // a decision anybody would defend afterwards.
                    // NAMED, because six of these a day on production were uncorrelatable with anything:
                    // the line said what happened and gave nothing to look it up by. The family id joins
                    // to auth_session, which then supplies the person, the tier, when the session was
                    // issued and when it last renewed. The presented generation is the one fact that row
                    // CANNOT supply — it holds the family's current generation, so the pair is what says
                    // how far behind the holder was, and a holder several generations behind is a
                    // different story from one behind by a single rotation.
                    //
                    // The family id is a random identifier and not personal data; everything personal
                    // stays in the table, reachable only by somebody who can already query it.
                    Console.log("🛡 A retired identity token was presented — ending the whole session family."
                                + " A copy of it is in someone else's hands. family=" + presented.familyId()
                                + " presentedGeneration=" + presented.generation());
                    yield revokeAndRemember(store, presented.familyId(), "reuse-detected")
                        .otherwise(e -> {
                            Console.log("⚠️ Could not record the revocation of a reused session family: " + e);
                            return null;
                        })
                        .map(ignored -> TokenRenewal.ENDED);
                }
                case ENDED -> Future.succeededFuture(TokenRenewal.ENDED);
                case UNDECIDED -> Future.succeededFuture(TokenRenewal.KEEP);
            })
            // THE DATABASE NOT ANSWERING IS NOT A VERDICT. Ending the session here would make any blip on
            // this table a mass logout, which is the failure this whole policy exists to prevent.
            .otherwise(e -> {
                Console.log("⚠️ Could not read a session family, so its token stands unchanged: " + e);
                return TokenRenewal.KEEP;
            });
    }

    /**
     * Ends the session family behind the call being handled — what a logout has to do beyond forgetting.
     *
     * <p>A logout drops the token on the device that asked for it, and until now did nothing else. The family
     * stayed live, so a COPY of that token — the exact thing rotation exists to catch — kept working for the
     * rest of its access window and stayed renewable for the whole idle window: three hours for the back
     * office, ninety days for a member. Logging out is precisely the moment somebody says "end this", and
     * ending it only on their own device is the weakest possible reading of that.
     *
     * <p>Two consequences worth knowing rather than discovering:
     *
     * <ul>
     *   <li><b>It ends the session in the user's OTHER tabs too</b>, within their access window, because tabs
     *       of one browser share one stored token and therefore one family. That is what people mean by
     *       logging out, and it is not what happened before.</li>
     *   <li><b>It does not reach their other devices</b>, which logged in separately and hold families of
     *       their own. Signing out everywhere is a different act, and the {@code revoked} column is where it
     *       will live.</li>
     * </ul>
     *
     * <p>The family is read from the state of the call, where the syncer put it after verifying the token's
     * signature — never from anything the caller supplied. A caller who could name a family could end anyone
     * else's session by guessing an id, which would make this a denial-of-service primitive rather than a
     * logout.
     *
     * <p>Fail-soft: a logout must complete even if the row cannot be written. The alternative is a user who
     * asked to be signed out and got an error instead, still signed in, which is worse than a family that
     * outlives its usefulness and is swept an hour after it expires.
     */
    public static Future<Void> revokeCurrentSessionFamily() {
        String familyId = StateAccessor.getSessionFamilyId(ThreadLocalStateHolder.getThreadLocalState());
        SessionFamilyStore store = SessionFamilyStoreRegistry.getStore();
        // No family to end: a legacy token, a client that presented none, or a deployment with no store.
        // Nothing is wrong and nothing needs saying — this is most sessions on the day this ships.
        if (familyId == null || store == null)
            return Future.succeededFuture();
        // A store that THROWS rather than returning a failed future — a service not ready yet, an interceptor
        // refusing synchronously — must fail soft too. Caught here because otherwise() only sees a failed
        // future, and an exception escaping this call skips the rest of the logout: the device is never told,
        // and its session goes on naming the user.
        Future<Void> revocation;
        try {
            revocation = revokeAndRemember(store, familyId, "user");
        } catch (RuntimeException e) {
            revocation = Future.failedFuture(e);
        }
        return revocation
            .otherwise(e -> {
                Console.log("⚠️ Logged out, but could not revoke the session family — a copy of its token stays"
                            + " usable until its access window ends: " + e);
                return null;
            });
    }

    /**
     * Ends a family in the store AND tells this instance at once, so it refuses that family's tokens
     * without waiting to poll for its own write.
     *
     * <p>Noted only when the write succeeded: a revocation the store did not record is not a revocation,
     * and refusing the family here while the other instance keeps honouring it would be the worst of both
     * — a session that works or not depending on which task holds the socket.
     */
    private static Future<Void> revokeAndRemember(SessionFamilyStore store, String familyId, String reason) {
        return store.revoke(familyId, reason)
            .onSuccess(ignored -> RevokedFamilies.note(familyId, System.currentTimeMillis()));
    }

    /**
     * Recorded distinctly from a logout, which also writes "user".
     *
     * <p>Otherwise the one column that could answer "was I signed out, or did I sign myself out?" reads
     * the same either way — and that question is exactly what somebody asks when a device they were not
     * holding lands on a login screen.
     */
    private static final String SIGNED_OUT_ELSEWHERE_REASON = "signed-out-elsewhere";

    /**
     * Why every session of a person ended at once: their own word that the device they were signed in on is in
     * somebody else's hands. Distinct from the two above, because the rows are what a super administrator reads
     * when the same person asks to be let back in an hour later.
     */
    private static final String DEVICE_STOLEN_REASON = "device-stolen";

    /**
     * Ends every session of the caller except the one they are using — "sign out my other devices".
     *
     * <p><b>It takes no target, and that is the security of it.</b> Both the person and the session to
     * spare are read from the state the syncer wrote after this call's token verified. A {@code personId}
     * parameter would turn "sign out my devices" into "sign out anybody's", which for sequential ids
     * means everybody's — an unauthenticated denial of service against any member whose id can be
     * guessed, and they can all be guessed.
     *
     * <p><b>It requires a verified token, not a claim.</b> The family id is only ever set from a token
     * whose signature held, so demanding one is what stops a caller asserting somebody else's identity
     * — which a client can still do while the token flip is off — and ending their sessions with it.
     * A legacy token carries no family and is refused here too; it gets one at its next renewal, which
     * is minutes away, and nothing is lost by waiting.
     *
     * <p>Answers with the families it ended, so the caller can tell those devices at once rather than
     * leaving them to notice on their next message.
     */
    /**
     * Ends EVERY session of the caller, the one they are calling from included — the panic button's half of
     * "my device has been stolen".
     *
     * <p>Signing yourself out too is the point rather than a side effect: the premise of the control is that
     * stopping the thief matters more than keeping yourself working, and a session spared here is one the thief
     * might be holding — this call cannot tell which device is on the other end of which family.
     *
     * <p>Read from the verified token like {@link #revokeOtherSessionsOfCurrentUser}, takes no target for the
     * same reason, and refuses a support view for the same reason: its principal carries the VIEWED member's
     * person id, so unchecked it would end that member's sessions instead of the agent's.
     *
     * <p>It is only half of the control. Revocation ends the sessions in progress; it does nothing about the
     * thief signing back in, which is what disabling the account does (AccountSignInRestrictionStore). Either
     * alone leaves a way through — see the spec, and V0101.
     */
    public static Future<List<String>> endEverySessionOfCurrentUser() {
        return revokeFamiliesOfCurrentUser(false, DEVICE_STOLEN_REASON, "Ended every session of a person who reported their device stolen");
    }

    public static Future<List<String>> revokeOtherSessionsOfCurrentUser() {
        return revokeFamiliesOfCurrentUser(true, SIGNED_OUT_ELSEWHERE_REASON, "Ended %d other session(s) at their owner's request");
    }

    /**
     * The body of both controls above. {@code spareCurrentSession} is the only difference between them: "sign
     * out my other devices" keeps the caller signed in, the panic button does not.
     *
     * @param spareCurrentSession whether the family this call arrived on survives
     * @param reason              what the revoked rows record, which is what a rescue reads later
     * @param logMessage          counts only, never who — {@code %d} is the number of families ended
     */
    private static Future<List<String>> revokeFamiliesOfCurrentUser(boolean spareCurrentSession, String reason, String logMessage) {
        Object state = ThreadLocalStateHolder.getThreadLocalState();
        String currentFamilyId = StateAccessor.getSessionFamilyId(state);
        Object principal = StateAccessor.getUserId(state);
        SessionFamilyStore store = SessionFamilyStoreRegistry.getStore();
        if (currentFamilyId == null || principal == null)
            return Future.failedFuture("Ending sessions needs a session this server established itself");
        // A SUPPORT VIEW MUST NOT USE THIS. Its principal carries the viewed member's person id, so the
        // store would end that member's sessions — their phone, their laptop — while the agent's own
        // session, the one spared, is the agent's. A member signed out of everything by somebody else,
        // recorded as if they had done it themselves. The read-only restriction that exists for exactly
        // this principal cannot catch it either: these statements run as the server, which is what lets
        // a support view renew its own token at all.
        //
        // Fails closed on an unanswerable question, unlike the write gate: that one is global and must
        // not brick a deployment with no predicate registered, whereas this is one control that can
        // simply refuse.
        if (RestrictedPrincipalRegistry.isUserRestrictedOrUnknown(principal))
            return Future.failedFuture("A restricted session may not end the sessions of the person it is viewing");
        if (store == null) // nothing records sessions here, so there are no others to end
            return Future.succeededFuture(List.of());
        // Guarded for the reason revokeCurrentSessionFamily is: a store that throws rather than failing
        // its future — a service not ready, an interceptor refusing on the spot — must not escape as a
        // raw stack trace from a control a person just pressed.
        Future<List<String>> revocation;
        try {
            // "" spares nothing: `id <> ''` holds for every real family, so the caller's own goes too
            revocation = store.revokeOtherFamilies(principal, spareCurrentSession ? currentFamilyId : "", reason);
        } catch (RuntimeException e) {
            return Future.failedFuture(e);
        }
        return revocation
            .map(familyIds -> {
                long now = System.currentTimeMillis();
                // Refused on sight here from now on; the other instance learns at its next poll. Noted as
                // ONE batch because noting is also what announces the revocation to any of those sessions
                // connected here, and announcing per family would walk every connected client once per
                // device signed out.
                List<SessionFamilyStore.Revocation> revocations = new ArrayList<>(familyIds.size());
                for (String familyId : familyIds)
                    revocations.add(new SessionFamilyStore.Revocation(familyId, now));
                RevokedFamilies.noteAll(revocations);
                if (!familyIds.isEmpty())
                    // Counts only. Who it was is in the rows themselves — revoked, with a reason and a
                    // time — and naming a person in a log puts personal data somewhere with weaker
                    // controls than the table it came from.
                    Console.log("🛡 " + logMessage.replace("%d", String.valueOf(familyIds.size())));
                return familyIds;
            });
    }

    /**
     * Which lifetime policy a session being established right now falls under.
     *
     * <p>Decided once, at login, and signed into the token from then on. The {@code backoffice} flag it
     * uses is client-asserted, which is exactly why it is read ONCE rather than on every message: a
     * client can choose which tier its own session lands in at login — and choosing the shorter one only
     * shortens its own session — but it cannot change that choice afterwards, and cannot lengthen a
     * back-office session by starting to claim it is a front-office one. Making the read unnecessary
     * altogether is what putting the audience in the payload does next.
     *
     * <p><b>The flag is a parameter and not read from {@link ThreadLocalStateHolder} here, and that is
     * not a style choice.</b> Every gateway calls this from inside a {@code compose()} — after a
     * database round trip — and the thread-local state is restored the moment the synchronous part of
     * the call returns. Reading it here would answer "not the back office" for every login in the
     * system, silently, and give every staff session the front office's 90-day idle window and
     * year-long cap. The gateways already capture {@code runId} synchronously for the same reason; this
     * rides with it.
     */
    private static SessionTier tierForNewSession(Object principal, boolean backofficeSession) {
        // Safe to ask late: it takes the principal explicitly rather than reading the thread.
        if (RestrictedPrincipalRegistry.isUserRestricted(principal))
            return SessionTier.SUPPORT_VIEW;
        return backofficeSession ? SessionTier.BACK_OFFICE : SessionTier.FRONT_OFFICE;
    }

    /**
     * Mints, and returns null rather than throwing if it cannot.
     *
     * <p>Both callers sit on paths where an exception would cost a user their session for a reason that
     * has nothing to do with them — an unsignable principal type at login, or the same at renewal. Logged
     * in full, because either one is a defect rather than a condition.
     */
    private static String mintQuietly(Object principal, String familyId, int generation, SessionTier tier,
                                      SessionAudience audience, Set<SessionMethod> methods,
                                      long absoluteExpiryMillis, long nowMillis) {
        return mintQuietly(principal, familyId, generation, tier, audience, methods, absoluteExpiryMillis,
                           nowMillis, null);
    }

    /**
     * @param storeToTidy the store that just opened {@code familyId}, if this mint is the only thing
     *                    standing between that row and being useful. A family whose token never existed
     *                    is a row nobody will ever present, and it holds a person id and timestamps —
     *                    personal data — until its absolute expiry, which for a member is a year away.
     *                    Revoking it hands it to the retention sweep instead. Null where nothing was
     *                    opened, or where the family is one that already exists.
     */
    private static String mintQuietly(Object principal, String familyId, int generation, SessionTier tier,
                                      SessionAudience audience, Set<SessionMethod> methods,
                                      long absoluteExpiryMillis, long nowMillis,
                                      SessionFamilyStore storeToTidy) {
        try {
            return PrincipalToken.mint(principal, familyId, generation, tier, audience, methods,
                                       absoluteExpiryMillis, nowMillis);
        } catch (Exception e) {
            Console.log("⚠️ Could not mint an identity token for " + principal.getClass().getSimpleName() + ": " + e);
            if (storeToTidy != null && familyId != null)
                revokeAndRemember(storeToTidy, familyId, "never-issued")
                    .onFailure(err -> Console.log("⚠️ Could not tidy away an unused session family: " + err));
            return null;
        }
    }
}
