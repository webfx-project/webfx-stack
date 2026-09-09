package dev.webfx.stack.session.token;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.state.RestrictedPrincipalRegistry;

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
        if (principal == null || !SignedToken.isConfigured())
            return Future.succeededFuture(null);
        long now = System.currentTimeMillis();
        SessionTier tier = tierForNewSession(principal, backofficeSession);
        long absoluteExpiry = now + SessionLifetime.absoluteLifetimeMillis(tier);
        SessionFamilyStore store = SessionFamilyStoreRegistry.getStore();
        if (store == null)
            return Future.succeededFuture(mintQuietly(principal, null, 0, tier, absoluteExpiry, now));
        return store.open(principal, tier, absoluteExpiry)
            .map(familyId -> mintQuietly(principal, familyId, 0, tier, absoluteExpiry, now, store))
            .otherwise(e -> {
                Console.log("⚠️ Could not open a session family — this session cannot be rotated and a stolen"
                            + " copy of its token will not be detected: " + e);
                return mintQuietly(principal, null, 0, tier, absoluteExpiry, now);
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
                    mintQuietly(presented.principal(), null, 0, tier, absoluteExpiry, nowMillis)));
            return store.open(presented.principal(), tier, absoluteExpiry)
                .map(familyId -> TokenRenewal.renewed(
                    mintQuietly(presented.principal(), familyId, 0, tier, absoluteExpiry, nowMillis)))
                // Same reasoning as at login: a family that cannot be opened costs rotation, not the
                // session. Sliding it forward keeps the user working and the upgrade is retried later.
                .otherwise(e -> {
                    Console.log("⚠️ Could not open a session family while upgrading a token: " + e);
                    return TokenRenewal.renewed(
                        mintQuietly(presented.principal(), null, 0, tier, absoluteExpiry, nowMillis));
                });
        }

        if (store == null) {
            // The token names a family this server cannot look up. Only a misconfiguration reaches here —
            // a deployment that had a store and lost it — and the honest response is to keep the session
            // working while saying, loudly, that the generation is no longer being checked.
            Console.log("⚠️ A token names a session family but no store is registered — rotation and reuse"
                        + " detection are NOT in effect for it");
            return Future.succeededFuture(TokenRenewal.renewed(mintQuietly(presented.principal(),
                presented.familyId(), presented.generation(), tier, presented.absoluteExpiryMillis(), nowMillis)));
        }

        return store.renew(presented.familyId(), presented.generation(), nowMillis)
            .compose(renewal -> switch (renewal.verdict()) {
                case RENEWED, CURRENT -> Future.succeededFuture(TokenRenewal.renewed(
                    mintQuietly(presented.principal(), presented.familyId(), renewal.generation(), tier,
                        renewal.absoluteExpiryMillis(), nowMillis)));
                case REUSE_DETECTED -> {
                    // Two holders, and there is no way to tell from here which one is the member. Killing
                    // the family signs both out; leaving it alive keeps the thief in. Only one of those is
                    // a decision anybody would defend afterwards.
                    Console.log("🛡 A retired identity token was presented — ending the whole session family."
                                + " A copy of it is in someone else's hands.");
                    yield store.revoke(presented.familyId(), "reuse-detected")
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
                                      long absoluteExpiryMillis, long nowMillis) {
        return mintQuietly(principal, familyId, generation, tier, absoluteExpiryMillis, nowMillis, null);
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
                                      long absoluteExpiryMillis, long nowMillis, SessionFamilyStore storeToTidy) {
        try {
            return PrincipalToken.mint(principal, familyId, generation, tier, absoluteExpiryMillis, nowMillis);
        } catch (Exception e) {
            Console.log("⚠️ Could not mint an identity token for " + principal.getClass().getSimpleName() + ": " + e);
            if (storeToTidy != null && familyId != null)
                storeToTidy.revoke(familyId, "never-issued")
                    .onFailure(err -> Console.log("⚠️ Could not tidy away an unused session family: " + err));
            return null;
        }
    }
}
