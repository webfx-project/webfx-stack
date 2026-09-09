package dev.webfx.stack.session.token;

/**
 * What kind of session this is, for the purpose of deciding how long it may live.
 *
 * <p>One lifetime for every session is what makes the trade-off feel forced, because the sessions that
 * need aggressive expiry are not the ones watching a three-hour teaching. A back-office session opens
 * tens of thousands of records including health and accessibility notes; a member's session opens that
 * member's own data, and is the one that streams video for hours with no click to prove anyone is there.
 * Applying one number to both means either the staff session lives too long or the member is signed out
 * mid-teaching, which is the most visible failure this system has.
 *
 * <p><b>Decided when a credential is checked, and carried inside the signature from then on.</b> The only
 * thing distinguishing a back-office session today is a {@code backoffice} boolean the client sets and
 * sends, so a tier read from the live state on every message would be a tier the caller chooses. Reading
 * it once, at login, and signing it into the token makes it a fact for the life of the session. That is
 * also why this sits next to where the audience field will go: they are the same mechanism, and audience
 * is what finally makes the login-time read of {@code backoffice} unnecessary too.
 *
 * <p>Note what this is NOT. It says how long a session may last, never what it may do. "This is a
 * back-office session" and "this person may edit a booking" are different questions, and answering the
 * second from this enum is how a session kind quietly starts conferring powers.
 *
 * @author Claude Code
 */
public enum SessionTier {

    /**
     * A member, in the front office. Compromise costs one person's own data, and this is the only tier
     * that streams: the session must survive hours of playback with no interaction.
     */
    FRONT_OFFICE("fo"),

    /**
     * Staff, in the back office. The highest-value target in the system and the one that emits no media
     * heartbeat, so it is governed by idle time and expires briskly. Staff sign in daily; nobody watches
     * a teaching from the admin console.
     */
    BACK_OFFICE("bo"),

    /**
     * A member of staff holding a member's account open through a support pass. Already capped at thirty
     * minutes by the pass itself; the tier exists so nothing here can extend it past that.
     */
    SUPPORT_VIEW("sv");

    private final String code;

    SessionTier(String code) {
        this.code = code;
    }

    /** The short form written into the token payload. Short because every message carries it. */
    public String code() {
        return code;
    }

    /**
     * The tier a payload names, or null when it names none.
     *
     * <p>An unrecognised code answers {@link #BACK_OFFICE} — the shortest-lived tier — rather than the
     * most permissive. It can only arise from a token signed by a NEWER build that knows a tier this one
     * does not (a scanner app, say), and during that window the two honest options are to end such
     * sessions early or to run them for a year on a policy this build cannot see. Ending them early is
     * recoverable by logging in again; the other is not recoverable at all.
     */
    public static SessionTier fromCode(String code) {
        if (code == null)
            return null;
        for (SessionTier tier : values())
            if (tier.code.equals(code))
                return tier;
        return BACK_OFFICE;
    }
}
