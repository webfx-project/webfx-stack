package dev.webfx.stack.db.query.cli;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.ast.json.Json;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Reads the audience out of the session token this caller holds.
 *
 * <h3>Why the CLI must read it at all</h3>
 *
 * <p>The audience is what selects the back-office scope token over the member one — through
 * {@code ThreadLocalStateHolder.isEffectivelyBackoffice()}, which reads the VERIFIED audience, never the
 * client's claim. The client's claim is only a request made at login, and several things can make the server
 * grant a different one: a {@code backoffice} flag that did not reach the login message, a login path that
 * settles the audience some other way, a server old enough not to sign the claim. A run that asked for the
 * back office and silently got the front office would exercise the member arm, pass, and report that the
 * back-office arm is sound. That is the same failure shape as the probe blind spot of 20 November, and it is
 * the reason this class exists.
 *
 * <h3>Why reading our own token is a sound answer, and where the limit is</h3>
 *
 * <p>The token is {@code base64url(payload).expiryMillis.base64url(mac)} and the payload is plain JSON — a
 * client can read it, which is by design (it is a statement the server made TO the client). What the client
 * cannot do is change it: the MAC covers the payload, so the server either reads back exactly what this method
 * read, or rejects the token entirely and the caller has no identity at all. So for the one question asked
 * here — "which audience will the server read on my next message?" — the answer taken from our own copy is the
 * server's answer.
 *
 * <p><b>It is never a grant.</b> This verifies nothing: no key is available to a client, and none is wanted.
 * Its only two uses are to CONFIRM an audience before probing and to REFUSE when it cannot be confirmed. If
 * anything here is ever consulted to decide that a caller may do something, the signature check has been
 * skipped and the value is worth nothing.
 *
 * @author Claude Code
 */
public final class TokenAudience {

    /**
     * The key the server writes the audience under, {@code PrincipalToken.AUDIENCE_KEY}, which is private
     * there. {@code TokenAudienceCheck} mints with the real minter and reads back through here, so a rename on
     * that side fails a check rather than quietly producing "cannot tell".
     */
    private static final String AUDIENCE_KEY = "$aud";

    private static final char SEPARATOR = '.';

    private TokenAudience() {}

    /**
     * The audience signed into this token, or null when it cannot be read.
     *
     * <p>Null for every reason — no token, wrong shape, payload that is not JSON, no audience claim, an
     * audience code this build does not know — and the caller has one thing to do about all of them: refuse to
     * probe. Distinguishing them would invite treating some as benign, and none is: each means the tool cannot
     * say which arm it is about to test.
     *
     * <p>Note what is NOT inferred. A token minted before the claim existed carries none, and the server falls
     * back to its lifetime tier for those. That fallback is the server's to make on a token it has verified;
     * repeating it here from an unverified copy would be guessing an answer in the one place the whole point is
     * not to.
     */
    public static Audience of(String token) {
        if (token == null)
            return null;
        int firstSeparator = token.indexOf(SEPARATOR);
        if (firstSeparator <= 0)
            return null;
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(token.substring(0, firstSeparator)),
                                 StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return null; // not base64url: not a token this server minted, whatever else it is
        }
        try {
            ReadOnlyAstObject object = Json.parseObject(payload);
            return object == null ? null : Audience.ofTokenCode(object.getString(AUDIENCE_KEY));
        } catch (RuntimeException e) {
            return null; // signed by somebody, but not a payload we can read
        }
    }

    /**
     * Why this token does not confirm {@code requested}, or null when it does.
     *
     * <p>The message names both audiences, because the useful thing to see in a failed run is which arm was
     * actually about to be tested.
     */
    public static String refusal(String token, Audience requested) {
        Audience actual = of(token);
        if (actual == null)
            return "This session's token does not say which audience it was minted for, so the tool cannot"
                   + " confirm it is about to test the " + requested.optionValue() + " arm. Sign in again;"
                   + " if it persists, the server is not signing an audience into its tokens and this tool"
                   + " cannot be trusted to be testing the arm it names.";
        if (actual != requested)
            return "This session is a " + actual.optionValue() + " session, but " + requested.optionValue()
                   + " was asked for. Probing now would measure the " + actual.optionValue() + " arm and"
                   + " report it under the other one. Sign out and sign in again with --audience "
                   + requested.optionValue() + ".";
        return null;
    }
}
