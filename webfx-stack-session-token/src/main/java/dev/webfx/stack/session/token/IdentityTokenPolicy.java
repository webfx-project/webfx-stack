package dev.webfx.stack.session.token;

/**
 * Whether this server still accepts a caller's word for who it is.
 *
 * <p>Kept apart from {@link SignedToken} (which signs) and {@link PrincipalToken} (which encodes), because
 * this is neither: it is the single policy decision that turns the other two from decoration into
 * enforcement. Everything else in this package can be deployed with no visible effect, and that is the
 * point — the token rides along unused until this flag is turned on.
 *
 * <h3>Why this is a flag and not a code change</h3>
 *
 * <p>Flipping it logs out every client too old to hold a token, and how many that is cannot be known from
 * here — only from what is actually connected on the day. So the decision belongs to a person looking at
 * the client-version breakdown, taken in seconds and reversible in seconds, rather than to whoever happens
 * to be merging a branch. A deploy-time switch would force that judgement to be made weeks early, by
 * someone with strictly less information, and would make backing out a release.
 *
 * <p>It is also per environment, so staging can run required for a while before production does. That is
 * the only honest rehearsal available: the failure mode is old clients, and old clients only exist in the
 * wild.
 *
 * <h3>What flipping it does to a client that cannot hold a token</h3>
 *
 * <p>It does not log such a client out once — it makes it unable to stay logged in. The client is refused
 * for having no token, logs in perfectly successfully, is handed a token, discards it because it has no
 * field to keep it in, and is refused again on its next message. A login loop, which a page reload fixes
 * (the reload picks up a current build) and which nothing else does. That is why the tail matters and why
 * this defaults to off.
 *
 * @author Bruno Salmon
 */
public final class IdentityTokenPolicy {

    private IdentityTokenPolicy() {}

    /**
     * Off by default, and the default is the one that ships. A server that has never heard of this setting
     * behaves exactly as it did before, which is what lets the token be rolled out to clients over weeks
     * without any coordination.
     */
    private static boolean tokenRequired = false;

    /**
     * @param tokenRequired true to refuse an identity claimed without a valid token — see the class
     *                      documentation for what that costs and who should decide it
     */
    public static void setTokenRequired(boolean tokenRequired) {
        IdentityTokenPolicy.tokenRequired = tokenRequired;
    }

    /**
     * @return true if a claimed identity must be backed by a valid token to be honoured
     */
    public static boolean isTokenRequired() {
        return tokenRequired;
    }

    /**
     * The whole rule for a message that arrived without a token, in one place so it can be checked.
     *
     * <p>{@code claimsRealIdentity} is the half that is easy to get wrong, and getting it wrong is not a
     * small bug. A message carrying no user id, or the explicit logged-out sentinel, is ASSERTING NOTHING —
     * it is an anonymous visitor browsing a public page, or a client telling the server it has just logged
     * out. Refusing those would strip a claim that was never made: it would log out callers who were never
     * logged in, spend a log line on every anonymous front-office request, and interfere with the one path
     * where a user has explicitly asked to be signed out. Refusal applies only to a positive claim of being
     * somebody, which is the only thing here that needs proving.
     *
     * @param claimsRealIdentity true only if the message names an actual user — not null, not logged-out
     * @return true if that claim should be discarded
     */
    public static boolean refusesUntokenedClaim(boolean claimsRealIdentity) {
        return tokenRequired && claimsRealIdentity;
    }
}
