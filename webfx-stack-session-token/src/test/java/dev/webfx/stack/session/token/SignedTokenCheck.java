package dev.webfx.stack.session.token;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Adversarial check for {@link SignedToken}, written to break it rather than to watch it work.
 *
 * <p>No test framework: this repository declares no JUnit (modality-fork has it commented out as
 * time-consuming for the WebFX CLI), so this runs from main() and exits non-zero on failure. Run it
 * after any change here — a signing primitive that silently stops verifying fails open, and the failure
 * looks exactly like everything working.
 */
public class SignedTokenCheck {

    static int pass = 0, fail = 0;
    static final long NOW = 1_000_000L;
    static final byte[] KEY_A = "key-a-0123456789".getBytes(StandardCharsets.UTF_8);
    static final byte[] KEY_B = "key-b-0123456789".getBytes(StandardCharsets.UTF_8);

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    public static void main(String[] args) {
        System.out.println("round trip:");
        SignedToken.setKeys(List.of(KEY_A));
        String token = SignedToken.mint("person=42,account=7", NOW + 10_000);
        check("payload comes back intact", "person=42,account=7".equals(SignedToken.verify(token, NOW)));
        check("payload is not readable as plain text in the token", !token.contains("person=42"));

        System.out.println("tampering:");
        check("flipped last char of the MAC", SignedToken.verify(flipLast(token), NOW) == null);
        String[] parts = token.split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("person=1,account=1".getBytes(StandardCharsets.UTF_8));
        check("payload swapped, old MAC kept",
            SignedToken.verify(forgedPayload + "." + parts[1] + "." + parts[2], NOW) == null);
        check("expiry pushed out, old MAC kept",
            SignedToken.verify(parts[0] + "." + (NOW + 999_999) + "." + parts[2], NOW) == null);
        check("MAC removed entirely", SignedToken.verify(parts[0] + "." + parts[1], NOW) == null);
        check("empty MAC", SignedToken.verify(parts[0] + "." + parts[1] + ".", NOW) == null);
        check("garbage", SignedToken.verify("not-a-token", NOW) == null);
        check("null", SignedToken.verify(null, NOW) == null);

        System.out.println("expiry:");
        check("valid one millisecond before", SignedToken.verify(token, NOW + 9_999) != null);
        check("refused exactly at expiry", SignedToken.verify(token, NOW + 10_000) == null);
        check("refused after", SignedToken.verify(token, NOW + 10_001) == null);
        check("already-expired mint is refused", SignedToken.verify(SignedToken.mint("x", NOW - 1), NOW) == null);

        System.out.println("keys:");
        SignedToken.setKeys(List.of(KEY_B));
        check("another server's key does not verify", SignedToken.verify(token, NOW) == null);
        SignedToken.setKeys(List.of(KEY_B, KEY_A));
        check("rotation: old token still verifies on the retired key", SignedToken.verify(token, NOW) != null);
        String newToken = SignedToken.mint("fresh", NOW + 10_000);
        SignedToken.setKeys(List.of(KEY_B));
        check("rotation: new token minted with the head key", "fresh".equals(SignedToken.verify(newToken, NOW)));
        SignedToken.setKeys(List.of(KEY_A));
        check("rotation: once retired, the old key stops verifying", SignedToken.verify(newToken, NOW) == null);

        System.out.println("expired versus forged — we can tell them apart, a client still cannot:");
        // Keys are back to KEY_A here, which is what minted `token` (expiring at NOW + 10_000).
        check("ours and still current is not called expired", !SignedToken.isAuthenticButExpired(token, NOW));
        check("ours and past the deadline is called expired", SignedToken.isAuthenticButExpired(token, NOW + 10_000));
        check("ours, long past the deadline, still called expired", SignedToken.isAuthenticButExpired(token, NOW + 999_999));
        // Everything that is not ours must answer false, or "expired" becomes a way in: the caller uses this
        // to decide NOT to log someone out, so a forgery that answered true would keep an invented identity.
        check("garbage is not called expired", !SignedToken.isAuthenticButExpired("not-a-token", NOW + 10_000));
        check("null is not called expired", !SignedToken.isAuthenticButExpired(null, NOW + 10_000));
        check("empty is not called expired", !SignedToken.isAuthenticButExpired("", NOW + 10_000));
        check("flipped MAC is not called expired", !SignedToken.isAuthenticButExpired(flipLast(token), NOW + 10_000));
        check("payload swapped, old MAC kept, is not called expired",
            !SignedToken.isAuthenticButExpired(forgedPayload + "." + parts[1] + "." + parts[2], NOW + 10_000));
        check("expiry pushed out, old MAC kept, is not called expired",
            !SignedToken.isAuthenticButExpired(parts[0] + "." + (NOW + 999_999) + "." + parts[2], NOW + 10_000));
        check("MAC removed is not called expired", !SignedToken.isAuthenticButExpired(parts[0] + "." + parts[1], NOW + 10_000));
        SignedToken.setKeys(List.of(KEY_B));
        check("another server's expired token is not called ours", !SignedToken.isAuthenticButExpired(token, NOW + 10_000));
        SignedToken.setKeys(List.of(KEY_A));

        System.out.println("unconfigured:");
        SignedToken.setKeys(List.of());
        check("verify refuses everything", SignedToken.verify(token, NOW) == null);
        check("isAuthenticButExpired refuses everything", !SignedToken.isAuthenticButExpired(token, NOW + 10_000));
        check("isConfigured() is false", !SignedToken.isConfigured());
        boolean threw = false;
        try { SignedToken.mint("x", NOW + 1); } catch (IllegalStateException e) { threw = true; }
        check("mint throws rather than returning an unprotected token", threw);

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }

    static String flipLast(String s) {
        char c = s.charAt(s.length() - 1);
        return s.substring(0, s.length() - 1) + (c == 'A' ? 'B' : 'A');
    }
}
