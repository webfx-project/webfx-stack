package dev.webfx.stack.session.token;

/**
 * Alarm mode: for a while, every session in the system re-checks itself far more often than it is worth
 * doing normally.
 *
 * <p>It is raised when a credential is loose and nobody yet knows whose — the case where waiting to find
 * out is the expensive part. It is not for checking the revoked set, which
 * {@link RevokedFamilies} does on every message already and always. It is for accepting a cost that is not
 * worth paying the rest of the time.
 *
 * <h3>What this first version does</h3>
 *
 * <p>One thing: it shortens the access window to {@link #ACCESS_WINDOW_MILLIS}. Every token then has to be
 * exchanged within that window, and an exchange is a round trip to the session store — which is where a
 * revoked family, a retired generation and an ended session are all found out. So an alarm turns "this
 * session will notice within half an hour" into "within a couple of minutes", for every session at once,
 * for as long as it lasts. The other postures the design calls for — step-up on sensitive operations,
 * suspending magic-link login, louder logging on the identity path — are deliberately not here yet; they
 * are separate refusals, and each wants its own decision rather than riding in on this switch.
 *
 * <h3>An expiry, never a boolean</h3>
 *
 * <p>What is stored and polled is the moment the alarm lapses, not a flag that is on. A flag has to be
 * turned off by somebody, at the end of an incident, when nobody is thinking about the cost it is still
 * imposing — and a flag left on is indistinguishable from an alarm nobody has raised since. An expiry
 * ends by itself and is extended by raising it again, which is the behaviour that survives being
 * forgotten. Raising it while it is already raised simply moves that moment.
 *
 * <h3>Why this is held in memory and polled, not pushed</h3>
 *
 * <p>Instances do not hear each other. A message from a super administrator's browser lands on whichever
 * one holds that socket, so a pushed toggle would raise the alarm for roughly the share of clients that
 * instance serves, with no way to tell which. The expiry is written to the database and every instance
 * reads it on the same poll that reads revocations, which is how they already agree on anything.
 *
 * <p>The cost of being late is one poll interval of ordinary access windows — the same "costs promptness
 * and nothing else" that the revocation poll accepts, and for the same reason: nothing here is the
 * enforcement, it only decides how often the enforcement is consulted.
 *
 * <h3>The known edge: two instances that disagree</h3>
 *
 * <p>For as long as one instance holds an alarm and another has not yet read it, a client with tabs on both
 * renews on every message it sends: the alarmed one hurries its long token, and the other sees the short
 * token that comes back as near-expiry and renews it long again. Nothing breaks — no session ends, because
 * the losing generation lands inside the store's reuse grace — but it is a database write per message, in
 * the middle of an incident.
 *
 * <p>It is bounded by the poll interval rather than designed away, and deliberately: the alternative is to
 * make a token say which window it was minted under, which means a token format change for an edge that
 * lasts seconds. The poll is frequent, and its read is NOT shed under load, so the disagreement cannot
 * outlive a busy moment. {@code SecurityAlarmCheck} pins the behaviour of each side on its own.
 *
 * @author Claude Code
 */
public final class SecurityAlarm {

    private SecurityAlarm() {}

    /**
     * The access window while the alarm is raised.
     *
     * <p>Two minutes, which is the shortest window that is still an access window rather than a way of
     * renewing continuously: every message inside it goes through on the token it already holds, and the
     * exchange happens once. Shorter, and a client on a slow connection spends its time renewing.
     */
    public static final long ACCESS_WINDOW_MILLIS = 2 * 60 * 1000L;

    /**
     * How long one raise lasts.
     *
     * <p>Half an hour: long enough to cover "something is wrong and we are still finding out", short
     * enough that the cost it imposes ends on its own before anybody has to remember it. An incident that
     * outlasts it is one where somebody is still at the keyboard to raise it again, and a raise that
     * extends the expiry is the cheapest thing in this class.
     */
    public static final long RAISE_DURATION_MILLIS = 30 * 60 * 1000L;

    /**
     * When the alarm lapses, in epoch millis, or 0 for "not raised".
     *
     * <p>Volatile and nothing more: it is written by the poll and read by every message, it is one long,
     * and a reader that sees the previous value for a moment is a reader who applies the ordinary window
     * for one message longer.
     */
    private static volatile long expiryMillis;

    /** Whether the alarm is in force. */
    public static boolean isRaised(long nowMillis) {
        return nowMillis < expiryMillis;
    }

    /** When the alarm lapses, or 0 when it is not raised. Never in the past: a lapsed alarm reads as none. */
    public static long expiryMillis(long nowMillis) {
        long expiry = expiryMillis;
        return nowMillis < expiry ? expiry : 0;
    }

    /**
     * Records what the store says the expiry is — called by the poll, on every instance.
     *
     * <p>Takes the value as it stands rather than the later of the two, including backwards: an alarm
     * that was stood down early, or a row that turned out to be somebody's mistake, has to be able to
     * end. The store is the one place the answer lives; this is only a copy of it.
     *
     * @param storeExpiryMillis the expiry the store holds, or 0 when it holds none
     */
    public static void noteExpiry(long storeExpiryMillis) {
        expiryMillis = Math.max(0, storeExpiryMillis);
    }

    /** Forgets the alarm — for tests, and for a store that can no longer be read at all. */
    public static void clear() {
        expiryMillis = 0;
    }
}
