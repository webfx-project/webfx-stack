package dev.webfx.stack.db.querypush;

/**
 * What the identity-token machinery is doing right now, for the /monitor page.
 *
 * <p>Counts and settings only, and deliberately no identifiers: a family id names somebody's live
 * session. Served to back-office callers alone, because whether a token is required and whether
 * revocation is currently reaching this instance are worth something to somebody holding a stolen one.
 * The first is discoverable by sending a message without a token, but publishing it removes the cost of
 * asking and the chance of anyone noticing. What it is FOR is telling an operator, at a glance, whether
 * revocation is reaching this instance at all.
 *
 * <p>Published through {@link SessionSecurityMonitorRegistry} rather than read directly, so that the
 * monitor does not have to know about sessions, tokens or families. A deployment without the session
 * machinery registers nothing and this arrives null, like every other section added since the page
 * was written.
 *
 * @author Claude Code
 */
public final class SessionSecurityMonitorInfo {

    private final int revokedFamiliesHeld;
    private final long revocationsRefused;
    private final long lastRevocationPollAgeMillis;
    private final boolean tokenRequired;
    private final double sessionLifetimeScale;
    private final boolean revocationStoreRegistered;

    public SessionSecurityMonitorInfo(int revokedFamiliesHeld, long revocationsRefused,
                                      long lastRevocationPollAgeMillis, boolean tokenRequired,
                                      double sessionLifetimeScale, boolean revocationStoreRegistered) {
        this.revokedFamiliesHeld = revokedFamiliesHeld;
        this.revocationsRefused = revocationsRefused;
        this.lastRevocationPollAgeMillis = lastRevocationPollAgeMillis;
        this.tokenRequired = tokenRequired;
        this.sessionLifetimeScale = sessionLifetimeScale;
        this.revocationStoreRegistered = revocationStoreRegistered;
    }

    /** Sessions this instance will refuse on sight, revoked within the retention window. */
    public int getRevokedFamiliesHeld() {
        return revokedFamiliesHeld;
    }

    /**
     * Messages refused ON SIGHT since this task booted, because they named a revoked session.
     *
     * <p>Messages, not sessions: one tab that ignores its logout presents the same dead token on every
     * message it sends. And on sight only — a revoked session refused later, when its token failed to
     * renew, is not counted here, because that path also ends sessions that simply reached their bound
     * and the two must not be added together.
     */
    public long getRevocationsRefused() {
        return revocationsRefused;
    }

    /**
     * How long ago this instance last heard from the store, in millis, or -1 when it never has.
     *
     * <p>The figure worth watching. The poll is what carries a revocation from the instance that
     * performed it to the one holding the other half of the clients, so an age that keeps climbing
     * means this task is refusing revoked sessions only at renewal — minutes late instead of at once,
     * and silently, because everything else still works.
     */
    public long getLastRevocationPollAgeMillis() {
        return lastRevocationPollAgeMillis;
    }

    /** Whether this server refuses a caller that claims an identity without a token. */
    public boolean isTokenRequired() {
        return tokenRequired;
    }

    /** 1 in any real deployment. Anything else means a development build's test scale is in effect. */
    public double getSessionLifetimeScale() {
        return sessionLifetimeScale;
    }

    /**
     * Whether anything records session families here — which is what makes "never polled" readable.
     *
     * <p>Without it the two cases look identical and both look calm: a deployment that keeps no
     * families and never polls by design, and one that keeps them but whose poll has never once run.
     * The second is the failure this section exists to show.
     */
    public boolean isRevocationStoreRegistered() {
        return revocationStoreRegistered;
    }
}
