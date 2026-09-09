package dev.webfx.stack.session.token;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.stack.com.serial.SerialCodecManager;

/**
 * Turns an authenticated principal into a token the client can hold, and back again.
 *
 * <p>The principal is serialized with the same codecs that already carry it over the wire, so any
 * principal type an application registers is covered without this class knowing what any of them are.
 * That matters: the alternative — signing one known principal type — leaves every other type as the way
 * in, which is how the SSO principals stayed forgeable while the main one looked defended.
 *
 * <p><b>Decoding happens only after the signature holds.</b> {@code SerialCodecManager} dispatches on a
 * {@code $codec} string against a global registry with no per-field constraint, so decoding attacker-
 * supplied text chooses which class to instantiate from attacker-supplied input. Here the text has been
 * proved to be something this server wrote, so the codec id was ours too. Reversing these two steps
 * would be a far worse bug than the one this class exists to fix, and it would still pass every test
 * that only checks the happy path.
 *
 * <p>A verified principal equals the one that was minted, which is less obvious than it sounds. Signing
 * requires text, and a JSON text round trip does not preserve a number's boxed type — ids minted as
 * {@code Integer} come back as {@code Byte} for small values. That was harmless only because
 * {@code ModalityUserPrincipal} and {@code ModalityGuestPrincipal} were then given value equality; before
 * that, a principal did not equal itself across this class, which would have made the authorization cache
 * miss on every request and the session syncer read every message as a fresh login. If a future principal
 * type is added, give it value equality too, or it will fail the same way and fail silently.
 *
 * <h3>What the payload carries besides the principal, and why adding to it is safe</h3>
 *
 * <p>Session bookkeeping — the family this token belongs to, its generation in that family, its access
 * window, the absolute bound and the lifetime tier — rides in the SAME object as the encoded principal,
 * under keys prefixed with {@code $}, beside the {@code $codec} the codecs already write there.
 *
 * <p>That placement is the whole compatibility story, and it was chosen over the two alternatives on
 * purpose. Fields on the principal CLASS would collide with the value equality the authorization cache
 * and the syncer's user-switch detection both depend on: the same person in two apps would become two
 * principals. A wrapper object around the principal would be semantically tidier and would cost a
 * two-phase rollout, because during a blue/green deploy a token minted by the new build is presented to
 * an instance running the old one, which would fail to decode it — and a decode failure is not the
 * authentic-but-expired case the syncer tolerates, it is a logout. Sibling keys avoid both:
 *
 * <ul>
 *   <li><b>An older server reads a newer token</b> — it decodes the object as the principal, and the
 *       codecs are pull-based, so keys they do not ask for are ignored. It sees exactly what it saw
 *       before.</li>
 *   <li><b>A newer server reads an older token</b> — the keys are absent, which reads as a legacy token
 *       and is upgraded at its first renewal.</li>
 * </ul>
 *
 * <p>So the same property will hold for the audience field that comes next: adding to this payload does
 * not sign anyone out, in either direction, and does not need a two-phase rollout. The {@code $} prefix
 * is what keeps that true — it cannot collide with a codec's own field names, which are Java identifiers.
 *
 * @author Bruno Salmon
 */
public final class PrincipalToken {

    /** Session family id. Prefixed like {@code $codec} so it can never collide with a codec's own field. */
    private static final String FAMILY_ID_KEY = "$sid";
    /** Which link in the family's renewal chain this token is. */
    private static final String GENERATION_KEY = "$gen";
    /** When this token stops being usable without renewal, as epoch millis. */
    private static final String ACCESS_EXPIRY_KEY = "$exp";
    /** The outer bound renewal may never push past, as epoch millis. */
    private static final String ABSOLUTE_EXPIRY_KEY = "$abs";
    /** Which lifetime policy this session was opened under — see {@link SessionTier}. */
    private static final String TIER_KEY = "$tier";

    private PrincipalToken() {}

    /**
     * Mints a token asserting this principal, for one link of one session family.
     *
     * <p>Call this only where a credential has actually been verified, or where an earlier verification
     * is being carried forward by a renewal. A token is worth exactly what the check behind it was worth,
     * and one minted from an unverified claim is a signed lie — indistinguishable from the real thing,
     * and trusted the same way.
     *
     * @param familyId            the session family, or null when no store is keeping one
     * @param generation          this token's position in that family's chain
     * @param absoluteExpiryMillis the bound this session may never outlive
     * @param nowMillis           the minting time, passed in so lifetimes are testable without waiting
     */
    public static String mint(Object principal, String familyId, int generation, SessionTier tier,
                              long absoluteExpiryMillis, long nowMillis) {
        if (principal == null)
            return null;
        Object encoded = SerialCodecManager.encodeToJson(principal);
        if (!(encoded instanceof ReadOnlyAstObject))
            // A principal that serializes to a bare string or number carries no codec id, so it could not
            // be decoded back to its own type. Refuse rather than mint something unusable.
            throw new IllegalArgumentException("Principal type is not serializable to an object: " + principal.getClass());
        AstObject payload = copyOf((ReadOnlyAstObject) encoded);
        if (familyId != null) {
            payload.set(FAMILY_ID_KEY, familyId);
            payload.set(GENERATION_KEY, generation);
        }
        payload.set(ACCESS_EXPIRY_KEY, SessionLifetime.accessExpiryFrom(nowMillis));
        payload.set(ABSOLUTE_EXPIRY_KEY, absoluteExpiryMillis);
        payload.set(TIER_KEY, tier.code());
        return SignedToken.mint(Json.formatObject(payload), SessionLifetime.signedExpiryFrom(nowMillis, tier, absoluteExpiryMillis));
    }

    /**
     * What this server asserted, or null for anything it did not — forged, altered, past its signed
     * deadline, malformed, or minted under a key no longer accepted.
     *
     * <p>Null for every failure, deliberately: a caller has one thing to check, and cannot accidentally
     * treat "expired" as a lesser problem than "forged" when both mean the same thing here — this
     * request has no proven identity.
     *
     * <p>Note the asymmetry with the ACCESS window, which is not checked here. A token past its access
     * window but inside its signed deadline still comes back, because it is still this server's own
     * signed statement and is exactly what a renewal exchanges. Deciding whether it may be ACTED on is
     * {@link IdentityToken#isWithinAccessWindow}, and that decision belongs to the caller holding the
     * renewal path — not to the function that answers who signed what.
     */
    public static IdentityToken verify(String token, long nowMillis) {
        SignedToken.Verified verified = SignedToken.verified(token, nowMillis);
        if (verified == null)
            return null;
        ReadOnlyAstObject payload;
        Object principal;
        try {
            payload = Json.parseObject(verified.payload());
            principal = SerialCodecManager.decodeFromAstObject(payload);
        } catch (Exception e) {
            // Signed by us, but no longer decodable — a codec removed or renamed since it was minted.
            // Treated as no identity rather than propagated: the caller asked who this is, and the honest
            // answer is that we can no longer tell.
            return null;
        }
        if (principal == null)
            return null;
        SessionTier tier = SessionTier.fromCode(payload.getString(TIER_KEY));
        // A legacy token has no access window of its own, so its signed deadline is the whole of its life
        // — which is precisely how it behaved before this field existed.
        long accessExpiry = payload.getLong(ACCESS_EXPIRY_KEY, verified.expiryMillis());
        long absoluteExpiry = payload.getLong(ABSOLUTE_EXPIRY_KEY, verified.expiryMillis());
        return new IdentityToken(
            principal,
            payload.getString(FAMILY_ID_KEY),
            payload.getInteger(GENERATION_KEY, 0),
            accessExpiry,
            verified.expiryMillis(),
            absoluteExpiry,
            tier);
    }

    /**
     * A mutable copy, rather than writing into what the codec returned.
     *
     * <p>The encoder happens to hand back a fresh mutable object today, but a codec is free to return a
     * cached or shared one, and stamping session bookkeeping into a principal's shared encoding would
     * leak one session's family id into another's. Cheap, and it removes the question.
     */
    private static AstObject copyOf(ReadOnlyAstObject source) {
        AstObject copy = AST.createObject();
        ReadOnlyAstArray keys = source.keys();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.getString(i);
            Object value = source.get(key); // typed, so set() resolves to its Object overload rather than ambiguously
            copy.set(key, value);
        }
        return copy;
    }
}
