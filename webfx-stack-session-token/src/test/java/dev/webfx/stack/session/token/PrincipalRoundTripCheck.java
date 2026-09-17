package dev.webfx.stack.session.token;

import dev.webfx.stack.com.serial.SerialCodecManager;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Round-trips a principal through mint and verify, and checks what the token carries beside it.
 *
 * <p>Two of these once failed: a principal minted with Integer ids came back with Byte ids and did not
 * equals() itself. They pass because the principal was given value equality, NOT because the check was
 * relaxed to accommodate it. Keep it that way — the "same person and account" case is the one that
 * detects a principal type whose equality is by object identity, and such a type breaks the authorization
 * cache and the login-transition check without ever failing.
 *
 * <p>The session-bookkeeping cases are the ones that keep the payload wire-compatible in both directions.
 * If a codec ever started reading a {@code $}-prefixed key, or the session fields stopped being siblings
 * of the principal's own, a deploy would sign out everyone whose token was minted by the other half of a
 * blue/green pair — silently, because both halves would still verify the signature.
 *
 * <p>The principal here is {@link CheckPrincipal}, not an application's: this module must build without
 * the application above it. That Modality's own principals survive this round trip — the guest, and the
 * support view whose restriction must not be lost — is checked by ModalityPrincipalTokenCheck, in
 * modality-crm-server-authsession-plugin, where depending on both sides is legal.
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core: this repository declares no
 * JUnit. Run from main(); it exits non-zero while the issue stands.
 */
public class PrincipalRoundTripCheck {
    static int pass=0, fail=0;
    static final long NOW = 1_000_000L;
    static void check(String w, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   "+w); } else { fail++; System.out.println("  FAIL "+w); }
    }
    /** A token whose SESSION ends at NOW + 60s, whatever its access window says. */
    static String mint(Object principal) {
        return PrincipalToken.mint(principal, null, 0, SessionTier.FRONT_OFFICE, NOW + 60_000, NOW);
    }
    static Object principalOf(String token, long nowMillis) {
        IdentityToken identity = PrincipalToken.verify(token, nowMillis);
        return identity == null ? null : identity.principal();
    }
    public static void main(String[] a) {
        SerialCodecManager.registerSerialCodec(new CheckPrincipalSerialCodec());
        SignedToken.setKeys(List.of("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));

        System.out.println("a principal survives the round trip:");
        CheckPrincipal user = new CheckPrincipal(42, 7);
        String token = mint(user);
        Object back = principalOf(token, NOW);
        check("comes back as the same type", back instanceof CheckPrincipal);
        check("same person and account", user.equals(back));
        check("a different principal is NOT equal to it", !new CheckPrincipal(1, 1).equals(back));
        check("token does not expose the person id in the clear", !token.contains("42"));

        System.out.println("session bookkeeping rides beside the principal, not inside it:");
        String familyToken = PrincipalToken.mint(user, "fam-1", 4, SessionTier.BACK_OFFICE, NOW + 3_600_000, NOW);
        IdentityToken identity = PrincipalToken.verify(familyToken, NOW);
        check("the family comes back", "fam-1".equals(identity.familyId()));
        check("the generation comes back", identity.generation() == 4);
        check("the tier comes back", identity.tier() == SessionTier.BACK_OFFICE);
        check("the absolute bound comes back", identity.absoluteExpiryMillis() == NOW + 3_600_000);
        check("the principal is UNAFFECTED by the extra keys", user.equals(identity.principal()));
        check("it is not read as a legacy token", !identity.isLegacy());

        System.out.println("a token from before any of that still works — the blue/green case:");
        // Exactly what the previous implementation minted: the encoded principal and nothing else.
        String legacy = SignedToken.mint("{\"$codec\":\"CheckPrincipal\",\"personId\":42,\"accountId\":7}", NOW + 60_000);
        IdentityToken legacyIdentity = PrincipalToken.verify(legacy, NOW);
        check("still names the same principal", user.equals(legacyIdentity.principal()));
        check("is recognised as legacy", legacyIdentity.isLegacy());
        check("has no family to rotate", legacyIdentity.familyId() == null);
        check("its access window is its whole life, as it always was",
              legacyIdentity.accessExpiryMillis() == NOW + 60_000);

        System.out.println("forgery and expiry, through the principal layer:");
        check("expired token yields no principal", principalOf(token, NOW + 60_000) == null);
        check("altered token yields no principal",
              principalOf(token.substring(0, token.length()-1) + "X", NOW) == null);
        check("a hand-written principal is not a token", principalOf(
              "{\"$codec\":\"CheckPrincipal\",\"personId\":1,\"accountId\":1}", NOW) == null);
        SignedToken.setKeys(List.of("ffffffffffffffffffffffffffffffff".getBytes(StandardCharsets.UTF_8)));
        check("another server's key yields no principal", principalOf(token, NOW) == null);

        System.out.println("\n"+pass+" passed, "+fail+" failed");
        if (fail>0) System.exit(1);
    }
}
