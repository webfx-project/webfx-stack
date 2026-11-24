package dev.webfx.stack.db.query.cli;

/**
 * Which app a session was opened for, as the CLI names it on the command line and reads it back off a token.
 *
 * <p>The codes are the ones the server signs into the {@code $aud} claim — see {@code SessionAudience} in
 * {@code webfx-stack-session-token}, which this deliberately does not depend on (a client holds a token as an
 * opaque string and has no business linking against the minting side). {@code TokenAudienceCheck} mints real
 * tokens with the real minter and reads them back through {@link TokenAudience}, so the two vocabularies
 * cannot drift apart without a check failing.
 *
 * @author Claude Code
 */
public enum Audience {

    /** The front office: members, bookings, the public site. {@code --audience front-office}. */
    FRONT_OFFICE("fo", "front-office", false),

    /** The back office: staff screens, and everything a route grant reaches. {@code --audience back-office}. */
    BACK_OFFICE("bo", "back-office", true);

    private final String tokenCode;
    private final String optionValue;
    private final boolean backofficeFlag;

    Audience(String tokenCode, String optionValue, boolean backofficeFlag) {
        this.tokenCode = tokenCode;
        this.optionValue = optionValue;
        this.backofficeFlag = backofficeFlag;
    }

    /** The short form the server signs into the token. */
    public String tokenCode() {
        return tokenCode;
    }

    /** What the operator types after {@code --audience}. */
    public String optionValue() {
        return optionValue;
    }

    /**
     * The value of the {@code backoffice} client flag that ASKS for this audience at login.
     *
     * <p>Asking is all it is. The server decides, and signs its decision into the token; the CLI then reads
     * the decision back rather than assuming the request was granted — see {@link TokenAudience} and
     * {@code CallerSession.signIn}.
     */
    public boolean backofficeFlag() {
        return backofficeFlag;
    }

    /** The audience this option value names, or null when it names none. */
    public static Audience ofOptionValue(String value) {
        if (value != null)
            for (Audience audience : values())
                if (audience.optionValue.equals(value))
                    return audience;
        return null;
    }

    /** The audience this token code names, or null when it names none. */
    public static Audience ofTokenCode(String code) {
        if (code != null)
            for (Audience audience : values())
                if (audience.tokenCode.equals(code))
                    return audience;
        return null;
    }
}
