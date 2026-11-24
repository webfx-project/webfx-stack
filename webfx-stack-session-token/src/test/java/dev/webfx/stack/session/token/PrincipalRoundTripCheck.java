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
        return PrincipalToken.mint(principal, null, 0, SessionTier.FRONT_OFFICE, SessionAudience.FRONT_OFFICE,
                                   SessionMethod.of(SessionMethod.PASSWORD), NOW + 60_000, NOW);
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
        String familyToken = PrincipalToken.mint(user, "fam-1", 4, SessionTier.BACK_OFFICE,
                                                 SessionAudience.BACK_OFFICE,
                                                 SessionMethod.of(SessionMethod.PASSWORD, SessionMethod.TOTP),
                                                 NOW + 3_600_000, NOW);
        IdentityToken identity = PrincipalToken.verify(familyToken, NOW);
        check("the family comes back", "fam-1".equals(identity.familyId()));
        check("the generation comes back", identity.generation() == 4);
        check("the tier comes back", identity.tier() == SessionTier.BACK_OFFICE);
        check("the audience comes back", identity.audience() == SessionAudience.BACK_OFFICE);
        check("both methods come back, and only those",
              identity.methods().equals(SessionMethod.of(SessionMethod.PASSWORD, SessionMethod.TOTP)));
        check("the absolute bound comes back", identity.absoluteExpiryMillis() == NOW + 3_600_000);
        check("the principal is UNAFFECTED by the extra keys", user.equals(identity.principal()));
        check("it is not read as a legacy token", !identity.isLegacy());

        // The reason the audience is its own field and not read off the tier, and the ONLY shape that can
        // prove the claim is being read at all: a pair the tier fallback would get WRONG. Where the two
        // agree — a bo tier with a bo audience — an implementation that never writes $aud passes just the
        // same, because ofLegacyTier would guess it correctly. A support view on a back-office connection
        // is the real divergence: thirty minutes of life, and back-office privileges. Do not "simplify"
        // this pair to a matching one; that is how both of these stop testing anything.
        System.out.println("the audience is read from the signature, not guessed from the tier:");
        String supportToken = PrincipalToken.mint(user, "fam-2", 0, SessionTier.SUPPORT_VIEW,
                                                  SessionAudience.BACK_OFFICE,
                                                  SessionMethod.of(SessionMethod.SUPPORT_VIEW_PASS),
                                                  NOW + 1_800_000, NOW);
        IdentityToken support = PrincipalToken.verify(supportToken, NOW);
        check("a support-view tier keeps its own short life", support.tier() == SessionTier.SUPPORT_VIEW);
        check("and its audience is the one it was minted with, which the tier does NOT imply",
              support.audience() == SessionAudience.BACK_OFFICE);

        System.out.println("the methods are a set, canonically encoded, and never inferred:");
        // One set, one encoding, whatever order the caller built it in — so two tokens for the same login
        // are byte-identical in this field, and a diff of two payloads means something.
        check("declaration order, not the caller's",
              "pwd,totp".equals(SessionMethod.joinCodes(
                  SessionMethod.of(SessionMethod.TOTP, SessionMethod.PASSWORD))));
        check("nothing to say is written as nothing at all, not as an empty claim",
              SessionMethod.joinCodes(SessionMethod.NONE) == null);
        // A method a NEWER instance signed and this build has never heard of. Skipped, which errs the
        // safe way: the set comes back smaller, so a reader looking for a strong factor finds less than
        // is really there and falls back. Treating it as a factor would let a rename grant what it never
        // meant to.
        check("a method this build does not know is skipped, not guessed at",
              SessionMethod.parseCodes("pwd,quantum-retina").equals(SessionMethod.of(SessionMethod.PASSWORD)));
        check("and a claim of nothing but unknowns reads as saying nothing",
              SessionMethod.parseCodes("quantum-retina").isEmpty());
        // The asymmetry with $aud, and it is deliberate: the audience has the tier to fall back on, the
        // methods have nothing. Inferring "password" because that used to be the common case would
        // manufacture the very fact a policy is asking about.
        IdentityToken noMethods = PrincipalToken.verify(
            PrincipalToken.mint(user, null, 0, SessionTier.FRONT_OFFICE, SessionAudience.FRONT_OFFICE,
                                SessionMethod.NONE, NOW + 60_000, NOW), NOW);
        check("a token naming no methods says so, rather than being given a likely one",
              noMethods.methods().isEmpty());

        // What a token minted by the other half of a blue/green pair looks like: tiers but no $aud yet.
        // Here the fallback IS the right answer, and this pins which way it falls.
        System.out.println("a token with a tier but no audience falls back to the tier:");
        String noAud = SignedToken.mint("{\"$codec\":\"CheckPrincipal\",\"personId\":42,\"accountId\":7,"
                                        + "\"$tier\":\"" + SessionTier.BACK_OFFICE.code() + "\"}", NOW + 60_000);
        check("a back-office tier reads as a back-office audience",
              PrincipalToken.verify(noAud, NOW).audience() == SessionAudience.BACK_OFFICE);

        System.out.println("a token from before any of that still works — the blue/green case:");
        // Exactly what the previous implementation minted: the encoded principal and nothing else.
        String legacy = SignedToken.mint("{\"$codec\":\"CheckPrincipal\",\"personId\":42,\"accountId\":7}", NOW + 60_000);
        IdentityToken legacyIdentity = PrincipalToken.verify(legacy, NOW);
        check("still names the same principal", user.equals(legacyIdentity.principal()));
        check("is recognised as legacy", legacyIdentity.isLegacy());
        check("has no family to rotate", legacyIdentity.familyId() == null);
        check("its access window is its whole life, as it always was",
              legacyIdentity.accessExpiryMillis() == NOW + 60_000);
        // It carries no $aud, so the audience is read off the tier — and a legacy token has no tier
        // either. Front office is the answer that grants least, which is what an unknown must get.
        check("is read as a front-office session, the audience that grants least",
              legacyIdentity.audience() == SessionAudience.FRONT_OFFICE);
        check("and names no methods, which is not a claim that none were used",
              legacyIdentity.methods().isEmpty());

        // The OTHER direction of the blue/green contract, and the one no other case covers: an instance
        // that predates a claim reading a token that carries it. It cannot be tested by running the old
        // code, so it is tested in the form that matters — an unknown $-prefixed key must be inert. This
        // is what makes it safe to add the NEXT claim ($amr) without a flag day, and what would break if
        // a codec ever started reading keys it does not own.
        System.out.println("a claim this build has never heard of is inert — the other blue/green direction:");
        String fromTheFuture = SignedToken.mint(
            "{\"$codec\":\"CheckPrincipal\",\"personId\":42,\"accountId\":7,\"$tier\":\"fo\","
            + "\"$aud\":\"bo\",\"$amr\":[\"pwd\",\"totp\"],\"$whatever\":{\"nested\":true}}", NOW + 60_000);
        IdentityToken future = PrincipalToken.verify(fromTheFuture, NOW);
        check("the token still verifies", future != null);
        check("and still names the same principal", user.equals(future.principal()));
        check("the claims it DOES know are read correctly past the ones it does not",
              future.tier() == SessionTier.FRONT_OFFICE && future.audience() == SessionAudience.BACK_OFFICE);

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
