package dev.webfx.stack.session.token;

/**
 * How long a session lives, and how long the token that carries it is good for. Two different numbers,
 * and keeping them different is what makes the whole design work.
 *
 * <h3>Why there are two</h3>
 *
 * <p>Rotation only detects a stolen token if a retired one is refused whenever it is presented, which
 * read literally is a database round trip on the path that today costs one HMAC. That is how this design
 * usually dies. The escape is to make the <b>token</b> short-lived and the <b>session</b> long-lived:
 *
 * <ul>
 *   <li>The {@link #ACCESS_WINDOW_MILLIS access window} is how long a token may be USED. Short, and
 *       enforced from the signed payload, so per-message verification stays exactly what it is now —
 *       stateless HMAC, no database.</li>
 *   <li>The {@link #idleWindowMillis idle window} is how long the SESSION survives without the server
 *       hearing from it. It is the token's signed deadline, so a client that has been away for less than
 *       this can exchange its lapsed token for a fresh one; one that has been away longer cannot,
 *       because the signature has stopped verifying.</li>
 *   <li>The {@link #absoluteLifetimeMillis absolute lifetime} is the outer bound that renewal may never
 *       push past.</li>
 * </ul>
 *
 * <p>So the row in the database is read roughly once per session per twenty minutes rather than once per
 * message, a stolen token is good for at most the access window, and a thief who tries to renew either
 * races the real client — one of them presents a retired generation, and it is detected — or is caught
 * at the real client's next renewal.
 *
 * <h3>The numbers are policy</h3>
 *
 * <p>They are worth arguing about and are expected to move; the structure is the part worth holding to.
 * What is not negotiable is that there IS a renewal path, because a lifetime constant without one is not
 * a neutral default. Installing the signing key in production on 2026-08-26 was understood to be inert
 * until identity tokens were required, and instead imposed a hard twelve-hour cap on every session,
 * ending them mid-stream, mid-booking and mid-payment. See docs/security/session-lifetime-policy.md.
 *
 * @author Claude Code
 */
public final class SessionLifetime {

    private SessionLifetime() {}

    /**
     * How long a minted token may be used before it has to be exchanged for a new one.
     *
     * <p>This, not the frequency of the generation check, is what bounds how long a stolen token works.
     * Short enough that a copied token is worth little; long enough that the exchange is rare — at
     * thirty minutes a session costs roughly three database round trips an hour, against one HMAC per
     * message either way.
     */
    public static final long ACCESS_WINDOW_MILLIS = 30 * 60 * 1000L; // 30 minutes

    /**
     * How much of the access window may remain before a renewal is attempted, as a fraction.
     *
     * <p>Renewing in the last quarter rather than at the very end leaves room for the exchange to fail
     * and be retried on the next message while the current token is still perfectly usable. Renewing on
     * every message instead would put the database back on the hot path, which is the thing being
     * avoided.
     */
    public static final double RENEWAL_THRESHOLD_FRACTION = 0.25;

    private static final long MINUTE = 60 * 1000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    /**
     * How long a session survives with the server hearing nothing from it.
     *
     * <p>"Hearing nothing" is measured from server-observed traffic, never from anything the client
     * asserts about being active — the client already tells the server it is alive by sending messages,
     * so nothing new has to be trusted. The front office emits a media-consumption heartbeat every sixty
     * seconds while anything plays, which means a member listening to a recording at 2am renews
     * continuously without touching the device. That is deliberate and load-bearing, not incidental: it
     * is the difference between this policy working and it reproducing the incident it exists because
     * of. A later tidy-up that stops counting heartbeats as traffic would quietly restore the failure.
     *
     * <p><b>A protocol keepalive is NOT traffic for this purpose, and that is the other half of the same
     * decision.</b> The bus ping fires every thirty seconds for as long as a socket is open, whether or
     * not anybody is there; the Vert.x bridge handles it in its own branch and never runs the state sync
     * over it, so it cannot renew anything. Left as it is on purpose — routing ping state through the
     * syncer would look like a tidy-up and would silently make this window infinite, because a tab left
     * open on a locked laptop would renew itself indefinitely and no session would ever go idle again.
     * A ping says the socket is alive; only a message says somebody is using it.
     *
     * <p>The honest limit is that a heartbeat proves the DEVICE is playing, not that a PERSON is there.
     * A tab left playing overnight renews itself, which is why {@link #absoluteLifetimeMillis} still
     * matters, and why the back office — which emits no such signal — gets a brisk idle window instead.
     */
    public static long idleWindowMillis(SessionTier tier) {
        return switch (tier) {
            case FRONT_OFFICE -> 90 * DAY;
            case BACK_OFFICE -> 3 * HOUR;
            // Equal to the pass's own thirty-minute grant, so the session cannot outlive what authorised
            // it. Renewal within the window is still allowed, and still cannot push past the absolute
            // bound below, so a support view slides but never extends.
            case SUPPORT_VIEW -> 30 * MINUTE;
        };
    }

    /**
     * The outer bound on a session, which renewal may approach but never pass.
     *
     * <p>It is the only part of this policy that can still interrupt someone, since by definition it
     * cannot be extended. The answer to that is not a bigger number but a graceful ending — warn before
     * it lands, defer through the machinery that already defers reloads during playback, and
     * re-authenticate in place rather than bouncing to a login page and losing the page. That front-end
     * half is not built yet; until it is, a session that reaches this bound ends the way an expired one
     * always has.
     */
    public static long absoluteLifetimeMillis(SessionTier tier) {
        return switch (tier) {
            case FRONT_OFFICE -> 365 * DAY;
            case BACK_OFFICE -> DAY;
            case SUPPORT_VIEW -> 30 * MINUTE;
        };
    }

    /**
     * How long a just-retired generation is still accepted, and why this window has to exist at all.
     *
     * <p>Two browser tabs share one token, so they renew at roughly the same moment. Without a grace the
     * loser of that race presents a generation that has just been retired, which is indistinguishable
     * from the signal this whole mechanism exists to raise — and the session of a member with two tabs
     * open would be killed as a suspected theft. Inside the grace the loser is simply handed the current
     * token instead, with no increment, so both tabs converge.
     *
     * <p>It is a genuine weakening: a thief renewing within a minute of the real client is tolerated.
     * That is the trade, and it is the right way round — a false kill is certain and frequent, a theft
     * landing inside the same sixty seconds is neither.
     */
    public static final long REUSE_GRACE_MILLIS = 60 * 1000L; // 1 minute

    /** When a token minted now stops being usable without renewal. */
    public static long accessExpiryFrom(long nowMillis) {
        return nowMillis + ACCESS_WINDOW_MILLIS;
    }

    /**
     * The deadline to sign into a token minted now: the idle window, but never past the session's
     * absolute bound. Signing the shorter of the two is what makes the cap hold even for a client that
     * never comes back to be told about it.
     */
    public static long signedExpiryFrom(long nowMillis, SessionTier tier, long absoluteExpiryMillis) {
        return Math.min(nowMillis + idleWindowMillis(tier), absoluteExpiryMillis);
    }

    /** Whether a token whose access window ends at {@code accessExpiryMillis} is due to be renewed. */
    public static boolean isRenewalDue(long nowMillis, long accessExpiryMillis) {
        return accessExpiryMillis - nowMillis <= (long) (ACCESS_WINDOW_MILLIS * RENEWAL_THRESHOLD_FRACTION);
    }
}
