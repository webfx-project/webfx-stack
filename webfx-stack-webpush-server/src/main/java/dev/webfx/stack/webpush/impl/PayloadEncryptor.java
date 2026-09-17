package dev.webfx.stack.webpush.impl;

import nl.martijndwars.webpush.AbstractPushService;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Encrypted;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

import java.security.KeyFactory;
import java.security.Security;
import java.util.Base64;

/**
 * Encrypts a push payload per RFC 8291 ({@code aes128gcm} content encoding),
 * producing the binary body that goes directly into the push request to the
 * push service.
 * <p>
 * Uses {@link AbstractPushService#encrypt} from {@code nl.martijndwars:web-push}
 * for the actual ECDH key derivation + HKDF + AES-128-GCM encryption AND for
 * the RFC 8291 wire-frame assembly.
 *
 * <h2>Library API quirk — important</h2>
 * Despite its field name, {@link Encrypted#getCiphertext()} for
 * {@code AES128GCM} returns the COMPLETE wire frame
 * ({@code salt || rs || idlen || keyid || ciphertext}), not just the
 * encrypted bytes. The library's {@code HttpEce.encrypt(...)} concatenates
 * the header before returning (see line 90 of {@code HttpEce.java}), then
 * {@code AbstractPushService.encrypt(...)} stores that as {@code ciphertext}
 * in the {@link Encrypted} record.
 * <p>
 * That means {@code Encrypted#getSalt()} / {@code #getPublicKey()} are
 * redundant for AES128GCM (the header inside {@code getCiphertext()} already
 * carries them). We use {@code getCiphertext()} as the HTTP body directly —
 * any attempt to re-assemble a frame on top would produce a double-wrapped
 * payload that no recipient can decrypt (the failure mode is silent:
 * Chrome/Firefox drop the message without firing the SW {@code push} event).
 *
 * @author Bruno Salmon
 */
public final class PayloadEncryptor {

    /** EC point uncompressed marker (0x04) || 32-byte X || 32-byte Y = 65 bytes. */
    private static final int UNCOMPRESSED_P256_LENGTH = 65;

    static {
        // BouncyCastle is needed for the curve operations the library performs
        // internally. Registering the provider is idempotent — calling it
        // twice is a no-op. Done in a static initialiser so the first encrypt
        // call has it available.
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Encrypts {@code plaintext} for the given subscription's keys and returns
     * the complete {@code aes128gcm}-formatted body ready to POST to the push
     * service.
     *
     * @param plaintext       the application payload bytes (typically JSON-encoded)
     * @param p256dhKeyBase64 the subscription's {@code p256dh} key (65-byte
     *                        uncompressed point, base64url)
     * @param authKeyBase64   the subscription's {@code auth} secret (16 bytes, base64url)
     * @return the encrypted body — directly the HTTP request body, with the
     *         {@code Content-Encoding: aes128gcm} header set by the caller
     */
    public byte[] encrypt(byte[] plaintext, String p256dhKeyBase64, String authKeyBase64) {
        try {
            ECPublicKey clientKey = parseP256dhKey(p256dhKeyBase64);
            byte[] authSecret = Base64.getUrlDecoder().decode(stripPadding(authKeyBase64));

            Encrypted encrypted = AbstractPushService.encrypt(
                    plaintext, clientKey, authSecret, Encoding.AES128GCM);

            // See class Javadoc: for AES128GCM the library already assembled
            // the RFC 8291 frame inside getCiphertext(). Don't wrap it again.
            return encrypted.getCiphertext();
        } catch (Exception e) {
            throw new IllegalStateException("Web Push payload encryption failed", e);
        }
    }

    /**
     * Parses the subscription's {@code p256dh} key (base64url-encoded
     * 65-byte uncompressed EC point) into a BouncyCastle {@link ECPublicKey}.
     * The library's encrypt method takes the BC-specific interface (not the
     * standard {@code java.security.interfaces.ECPublicKey}), so we use BC's
     * KeyFactory rather than the default JCE one.
     */
    static ECPublicKey parseP256dhKey(String p256dhKeyBase64) {
        try {
            byte[] keyBytes = Base64.getUrlDecoder().decode(stripPadding(p256dhKeyBase64));
            if (keyBytes.length != UNCOMPRESSED_P256_LENGTH) {
                throw new IllegalArgumentException(
                        "Expected 65 bytes (uncompressed P-256 point); got " + keyBytes.length);
            }

            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
            org.bouncycastle.math.ec.ECPoint point = spec.getCurve().decodePoint(keyBytes);
            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(point, spec);
            return (ECPublicKey) KeyFactory.getInstance("EC", "BC").generatePublic(publicKeySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid subscription p256dh key", e);
        }
    }

    /** Some base64url encoders include padding (=); strip it to be tolerant. */
    private static String stripPadding(String base64Url) {
        int eq = base64Url.indexOf('=');
        return eq < 0 ? base64Url : base64Url.substring(0, eq);
    }
}
