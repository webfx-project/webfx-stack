package dev.webfx.stack.webpush.impl;

import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Builds and signs the VAPID JWT that every Web Push request needs in its
 * {@code Authorization} header (per RFC 8292).
 * <p>
 * The JWT is per-recipient-origin: the {@code aud} claim is the push service's
 * origin (e.g. {@code https://fcm.googleapis.com}, derived from each
 * subscription's endpoint URL). The same VAPID key pair signs JWTs for all
 * push services — only the audience differs.
 * <p>
 * Pure JDK crypto — no BouncyCastle or third-party library dependency. Uses
 * the {@code SHA256withECDSAinP1363Format} signature algorithm available since
 * JDK 11, which produces the raw {@code r||s} 64-byte signature that VAPID
 * mandates (the DER-encoded variant the JCE produces by default is NOT
 * accepted by push services).
 *
 * @author Bruno Salmon
 */
public final class VapidSigner {

    /** JWT validity window. Push services typically accept up to 24h; 1h is a safe default. */
    private static final long JWT_TTL_SECONDS = 60 * 60;

    /** {@code {"typ":"JWT","alg":"ES256"}} — constant for every push request. */
    private static final String JWT_HEADER_BASE64URL =
            base64Url("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));

    private final ECPrivateKey privateKey;
    private final String publicKeyBase64Url;
    private final String subject;

    /**
     * @param publicKeyBase64Url  the VAPID public key, base64url-encoded uncompressed point
     *                            (65 bytes raw: {@code 0x04 || X || Y}). Sent as the {@code k=}
     *                            parameter in the Authorization header.
     * @param privateKeyBase64Url the VAPID private key, base64url-encoded 32-byte scalar.
     * @param subject             contact URI ({@code mailto:...} or {@code https://...}); see RFC 8292.
     *                            Empty string is tolerated but pushed services may complain.
     */
    public VapidSigner(String publicKeyBase64Url, String privateKeyBase64Url, String subject) {
        this.publicKeyBase64Url = publicKeyBase64Url;
        this.privateKey = parsePrivateKey(privateKeyBase64Url);
        this.subject = subject == null ? "" : subject;
    }

    /**
     * Builds a fresh signed JWT for the audience derived from the given push
     * service URL. The audience is the origin (scheme + authority); the rest
     * of the path is irrelevant to VAPID.
     */
    public String signJwtFor(String endpointUrl) {
        try {
            String audience = originOf(endpointUrl);
            long expiry = Instant.now().plus(JWT_TTL_SECONDS, ChronoUnit.SECONDS).getEpochSecond();
            String claimsJson = "{\"aud\":\"" + escapeJson(audience)
                    + "\",\"exp\":" + expiry
                    + ",\"sub\":\"" + escapeJson(subject) + "\"}";
            String claimsBase64 = base64Url(claimsJson.getBytes(StandardCharsets.UTF_8));
            String signingInput = JWT_HEADER_BASE64URL + "." + claimsBase64;

            Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();

            return signingInput + "." + base64Url(signatureBytes);
        } catch (Exception e) {
            // Catch-all because the JCE throws several checked exceptions for invariants
            // that should never fail at runtime once configuration validation has passed.
            throw new IllegalStateException("VAPID JWT signing failed", e);
        }
    }

    /** Exposed so the service can include the public key in the Authorization header's {@code k=} parameter. */
    public String publicKeyBase64Url() {
        return publicKeyBase64Url;
    }

    // ── Internals ────────────────────────────────────────────────────────────

    /** Decodes a base64url 32-byte scalar into an EC private key on the P-256 (secp256r1) curve. */
    private static ECPrivateKey parsePrivateKey(String privateKeyBase64Url) {
        try {
            byte[] scalarBytes = Base64.getUrlDecoder().decode(stripPadding(privateKeyBase64Url));
            BigInteger s = new BigInteger(1, scalarBytes); // unsigned

            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
            params.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);

            ECPrivateKeySpec keySpec = new ECPrivateKeySpec(s, ecSpec);
            return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid VAPID private key (expected base64url-encoded P-256 scalar)", e);
        }
    }

    /** Extract the origin (scheme + authority) from the endpoint URL — used as the JWT {@code aud}. */
    private static String originOf(String endpointUrl) {
        URI uri = URI.create(endpointUrl);
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    /** Minimal JSON-string escaper — covers the only special characters that can appear in VAPID claims. */
    private static String escapeJson(String s) {
        // mailto: addresses and HTTPS URIs don't contain " or \ in practice, but be defensive.
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Base64url encode without padding (per RFC 7515 §2 — what JWT mandates). */
    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Some VAPID generators include padding (=); strip it to be tolerant. */
    private static String stripPadding(String base64Url) {
        int eq = base64Url.indexOf('=');
        return eq < 0 ? base64Url : base64Url.substring(0, eq);
    }
}
