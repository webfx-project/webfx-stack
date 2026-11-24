package dev.webfx.stack.session.token;

/**
 * Which app a session was established FOR — the {@code $aud} claim, signed into the token at login.
 *
 * <h3>What it is, and what it is not</h3>
 *
 * <p>It is NOT proof of where a caller is. Only a passkey sign-in yields that: the origin comes out of
 * the signature-covered {@code clientDataJSON} and the gateway refuses a mismatch. Every other path —
 * password, magic link, verification code, the second factor's completion, the guest cart — can take it
 * only from the {@code backoffice} flag the client itself asserts.
 *
 * <p>What makes it worth having is that the assertion is made ONCE and then frozen. Today that flag is
 * re-asserted on every message, so a caller can claim {@code fo} at login — skipping the second factor,
 * the back-office account check and the three-hour tier — and then claim {@code bo} afterwards to use
 * back-office privileges. With the claim inside the signature it cannot be changed later: to hold a
 * {@code bo} session you must say {@code bo} at login, which is exactly when those conditions fire.
 *
 * <p><b>The claim and its consequences arrive together, and cannot be separated afterwards.</b> That is
 * the whole security property, and it is a different and weaker one than "we know which app this is" —
 * do not let the name suggest otherwise.
 *
 * <h3>Why not reuse SessionTier</h3>
 *
 * <p>They share a vocabulary and answer different questions. The tier says how long a session may live;
 * the audience says what it was opened for. A support view is {@code SUPPORT_VIEW} for lifetime (thirty
 * minutes) and belongs to the FRONT office for audience, so one field cannot carry both. Where a token
 * predates this claim the tier does stand in for it — see {@link #ofLegacyTier} — because for the two
 * values they do share it was derived from the same flag.
 *
 * @author Claude Code
 */
public enum SessionAudience {

    /** The front office: members, bookings, the public site. */
    FRONT_OFFICE("fo"),

    /** The back office: staff screens, and everything the route grants reach. */
    BACK_OFFICE("bo");

    private final String code;

    SessionAudience(String code) {
        this.code = code;
    }

    /** The short form that rides in the token, kept stable because old tokens carry it. */
    public String code() {
        return code;
    }

    /** The audience a connection claiming (or proving) this flag is asking for. */
    public static SessionAudience ofBackofficeFlag(boolean backoffice) {
        return backoffice ? BACK_OFFICE : FRONT_OFFICE;
    }

    /**
     * What a token minted before this claim existed should be read as.
     *
     * <p>The tier carries {@code fo} and {@code bo} already, derived at login from the same flag, so for
     * those two it is the same answer by a different name. A {@code SUPPORT_VIEW} tier maps to the FRONT
     * office, which is where a support view belongs — and such a session ends within thirty minutes
     * regardless, so nothing rests on it for long.
     *
     * <p>Null in, front office out: a token with no tier either is older still, and the front office is
     * the audience that grants least.
     */
    public static SessionAudience ofLegacyTier(SessionTier tier) {
        return tier == SessionTier.BACK_OFFICE ? BACK_OFFICE : FRONT_OFFICE;
    }

    /**
     * The audience a code names, or null when it names none.
     *
     * <p>Null rather than a default, because the caller knows what an absent claim means and this does
     * not: at mint time there is always one, and at verify time an absent one is a legacy token that
     * {@link #ofLegacyTier} answers from the tier.
     */
    public static SessionAudience fromCode(String code) {
        if (code == null)
            return null;
        for (SessionAudience audience : values())
            if (audience.code.equals(code))
                return audience;
        return null;
    }
}
