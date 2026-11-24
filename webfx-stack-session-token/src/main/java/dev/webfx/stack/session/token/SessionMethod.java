package dev.webfx.stack.session.token;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * How a session was authenticated — the {@code $amr} claim, signed into the token at login.
 *
 * <h3>What it is for</h3>
 *
 * <p>A session is worth what the credentials behind it were worth, and until now the token said nothing
 * about them: a password-only session and a password-plus-passkey session were indistinguishable once
 * minted. Anything that wants to insist on a strong factor — the back-office second-factor policy, and
 * the rule that a passkey may only be enrolled from a session that already has one — has to know which
 * methods were actually presented, and has to know it from something the caller cannot restate.
 *
 * <p>So this is a record of what happened, frozen at the moment it happened. It decides nothing by
 * itself, which is deliberate: the policy that reads it lives above this module and changes on its own
 * schedule.
 *
 * <h3>Reading it, and the two traps</h3>
 *
 * <p><b>An empty set means "this token does not say", not "no factors were used".</b> Every token minted
 * from here on names at least one method, but one minted before this claim existed names none, and so
 * does one from a future build whose methods this one has never heard of. A reader deciding whether a
 * strong factor is present must treat an empty or unrecognised set as "nothing is proven" and fall back
 * to whatever it did before the claim existed — never as a session that passed no checks, and never as
 * one to be let through because the set looks tidy.
 *
 * <p><b>It is frozen at login and a renewal carries it forward unchanged.</b> A session therefore cannot
 * gain a method by staying open: enrolling a second factor at 11am does not add {@code totp} to a
 * session minted at 9am, and nothing here will tell a policy otherwise. That is the honest behaviour —
 * the session genuinely was opened with what it was opened with — but a step-up that is meant to take
 * effect immediately needs a fresh mint, not a renewal.
 *
 * @author Claude Code
 */
public enum SessionMethod {

    /** A password was checked. The weakest of these, and the one the others are a second step to. */
    PASSWORD("pwd"),

    /** A WebAuthn assertion: a signature over a server-issued challenge, by a key this account owns. */
    PASSKEY("pk"),

    /** A time-based code from an authenticator app. */
    TOTP("totp"),

    /** A sign-in link, clicked from an email. Proves control of the address, at the time it was sent. */
    MAGIC_LINK("ml"),

    /** A verification code, typed from an email. The same proof as {@link #MAGIC_LINK}, differently delivered. */
    VERIFICATION_CODE("code"),

    /** A cart's bearer link, redeemed by a guest who has no account to sign in to. */
    CART_LINK("cart"),

    /** A one-time support-view pass, minted for an agent who held the permission and authenticated separately. */
    SUPPORT_VIEW_PASS("sv"),

    /** A one-time back-office-view pass, minted by a super administrator who authenticated separately. */
    BACKOFFICE_VIEW_PASS("bov");

    private final String code;

    SessionMethod(String code) {
        this.code = code;
    }

    /** The short form that rides in the token, kept stable because old tokens carry it. */
    public String code() {
        return code;
    }

    /** No methods named — a legacy token, or none recognised. Never a statement that none were used. */
    public static final Set<SessionMethod> NONE = Collections.emptySet();

    /** The set holding exactly these, in declaration order, so one set has exactly one encoding. */
    public static Set<SessionMethod> of(SessionMethod... methods) {
        if (methods.length == 0)
            return NONE;
        EnumSet<SessionMethod> set = EnumSet.noneOf(SessionMethod.class);
        for (SessionMethod method : methods)
            if (method != null)
                set.add(method);
        return Collections.unmodifiableSet(set);
    }

    /**
     * The claim's value for these methods, or null for none — which is how an absent claim is written.
     *
     * <p>Declaration order, not insertion order, so the same methods always produce the same string and
     * two tokens for the same login are byte-identical in this field. {@code EnumSet} gives that for
     * free; a {@code HashSet} would not.
     */
    public static String joinCodes(Set<SessionMethod> methods) {
        if (methods == null || methods.isEmpty())
            return null;
        StringBuilder joined = new StringBuilder();
        // Iterating the enum rather than the set, so the order is the declaration's whatever Set arrives.
        for (SessionMethod method : values())
            if (methods.contains(method)) {
                if (joined.length() > 0)
                    joined.append(',');
                joined.append(method.code);
            }
        return joined.length() == 0 ? null : joined.toString();
    }

    /**
     * The methods a claim names, skipping any this build does not recognise.
     *
     * <p>Skipping rather than failing, and it errs the safe way: a method a newer instance signed and
     * this one has never heard of leaves the set SMALLER, so a reader looking for a strong factor finds
     * less than is really there and falls back rather than letting something through. The reverse —
     * treating an unknown code as a factor — would let a future rename grant what it never meant to.
     */
    public static Set<SessionMethod> parseCodes(String claim) {
        if (claim == null || claim.isEmpty())
            return NONE;
        EnumSet<SessionMethod> parsed = EnumSet.noneOf(SessionMethod.class);
        for (String code : claim.split(",")) {
            String trimmed = code.trim();
            for (SessionMethod method : values())
                if (method.code.equals(trimmed)) {
                    parsed.add(method);
                    break;
                }
        }
        return parsed.isEmpty() ? NONE : Collections.unmodifiableSet(parsed);
    }
}
