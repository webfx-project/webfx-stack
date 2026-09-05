package dev.webfx.stack.session.token;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

/**
 * A short string that says something the server asserted, which a client can hold but not alter.
 *
 * <p>The problem it solves: a server that hands a client a fact and reads it back has told the client
 * what to say. Signing the fact makes the round trip safe — the client stores an opaque string, echoes
 * it, and the server recovers what it originally said or nothing at all.
 *
 * <p>Format is {@code base64url(payload).expiryMillis.base64url(mac)}, where the MAC covers
 * {@code base64url(payload).expiryMillis} — so the expiry is signed too, and a client cannot postpone
 * its own deadline by editing the field it can see.
 *
 * <p>Deliberately knows nothing about identity. It signs a string; what that string means belongs to the
 * caller. That keeps this class testable without a session, a principal or a running stack, and it is why
 * the payload is not JSON: anything that must be parsed to be verified invites parsing it before it has
 * been verified.
 *
 * <p><b>Verification order matters and is not an implementation detail.</b> The MAC is checked before the
 * expiry, and the payload is returned only after both. Nothing derived from an unverified token is ever
 * handed back, so a caller cannot accidentally act on an attacker's payload.
 *
 * @author Bruno Salmon
 */
public final class SignedToken {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final char SEPARATOR = '.';

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    /**
     * The keys this server signs and accepts with. The first mints; every entry verifies.
     *
     * <p>A list rather than one key so a rotation does not sign everyone out: publish the new key at the
     * head, keep the previous one until the longest possible token has expired, then drop it. With a
     * single key a rotation invalidates every live session at once, which tends to mean rotations do not
     * happen.
     */
    private static volatile List<byte[]> keys = List.of();

    private SignedToken() {}

    /**
     * Installs the signing keys. The first is used to mint; all are accepted when verifying.
     *
     * <p>Until this is called nothing can be minted and nothing verifies — {@link #mint} throws and
     * {@link #verify} returns null. An unconfigured server refuses to issue identities rather than
     * issuing unprotected ones.
     */
    public static void setKeys(List<byte[]> signingKeys) {
        keys = signingKeys == null ? List.of() : List.copyOf(signingKeys);
    }

    public static boolean isConfigured() {
        return !keys.isEmpty();
    }

    /**
     * Signs a payload with an absolute expiry.
     *
     * @param payload      what the server is asserting; opaque here
     * @param expiryMillis when it stops being true, as epoch milliseconds
     * @throws IllegalStateException if no signing key is configured — never returns an unsigned token
     */
    public static String mint(String payload, long expiryMillis) {
        List<byte[]> currentKeys = keys;
        if (currentKeys.isEmpty())
            throw new IllegalStateException("No signing key configured — refusing to mint an unprotected token");
        String signedPart = ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + SEPARATOR + expiryMillis;
        return signedPart + SEPARATOR + ENCODER.encodeToString(mac(currentKeys.get(0), signedPart));
    }

    /**
     * Returns the payload this server signed, or null for anything else.
     *
     * <p>Null covers every failure — no key, wrong shape, bad MAC, expired, undecodable — on purpose. A
     * caller has one thing to check, and cannot accidentally distinguish "wrong signature" from "expired"
     * in a way that tells an attacker which of the two they achieved.
     *
     * @param nowMillis the current time, passed in so expiry is testable without waiting for it
     */
    public static String verify(String token, long nowMillis) {
        Checked checked = check(token, nowMillis);
        return checked.validity() == Validity.VALID ? checked.payload() : null;
    }

    /**
     * True when this server really did sign the token and its expiry has simply passed.
     *
     * <p>Kept apart from {@link #verify} because the two answer different questions, and only one of them
     * is about trust. {@code verify} asks "may I act on this?", to which an expired token is a flat no.
     * This asks "did we write it?" — a fact about provenance, which stays true after the deadline.
     *
     * <p><b>It never yields the payload, and that is the point.</b> An expired token proves who someone
     * WAS; nothing here lets a caller recover the assertion and carry on as though it still held. The one
     * legitimate use is to tell a stale session apart from a forged one so that policy can treat them
     * differently — and that policy belongs to the caller, not to this class.
     *
     * <p>What this class hands back stays on this side of the wire, but do not read that as "the client
     * cannot tell". Whether the two become distinguishable depends entirely on what the caller DOES with
     * the answer: a caller that ends the session for one and not the other has built a MAC-validity oracle,
     * however quiet its responses are. That is a judgement for the caller to make and to write down — see
     * {@code ServerSideStateSessionSyncer.applyIdentityToken}, which makes it deliberately.
     */
    public static boolean isAuthenticButExpired(String token, long nowMillis) {
        return check(token, nowMillis).validity() == Validity.EXPIRED;
    }

    /** What a presented token turned out to be. Only {@link Validity#VALID} carries a payload. */
    private enum Validity { VALID, EXPIRED, INVALID }

    private record Checked(Validity validity, String payload) {
        static final Checked INVALID = new Checked(Validity.INVALID, null);
        static final Checked EXPIRED = new Checked(Validity.EXPIRED, null);
    }

    /**
     * The one reader of a presented token, so that "is this ours?" and "is it still good?" cannot drift
     * apart. Two parsers over the same attacker-supplied string is how one of them ends up accepting what
     * the other rejects.
     */
    private static Checked check(String token, long nowMillis) {
        List<byte[]> currentKeys = keys;
        if (token == null || currentKeys.isEmpty())
            return Checked.INVALID;
        int macSeparator = token.lastIndexOf(SEPARATOR);
        if (macSeparator < 0)
            return Checked.INVALID;
        String signedPart = token.substring(0, macSeparator);
        String presentedMac = token.substring(macSeparator + 1);
        int expirySeparator = signedPart.lastIndexOf(SEPARATOR);
        if (expirySeparator < 0)
            return Checked.INVALID;

        byte[] presented;
        try {
            presented = DECODER.decode(presentedMac);
        } catch (IllegalArgumentException e) {
            return Checked.INVALID;
        }

        // Signature first, always: everything below this point reads bytes the client supplied.
        boolean authentic = false;
        for (byte[] key : currentKeys)
            // Constant-time, and every key is tried even after a match — comparing with equals(), or
            // returning early, leaks through timing how much of the MAC was right, which is enough to
            // forge one byte at a time.
            authentic |= MessageDigest.isEqual(presented, mac(key, signedPart));
        if (!authentic)
            return Checked.INVALID;

        long expiryMillis;
        try {
            expiryMillis = Long.parseLong(signedPart.substring(expirySeparator + 1));
        } catch (NumberFormatException e) {
            return Checked.INVALID; // signed by us, but malformed: still refuse rather than guess
        }
        if (nowMillis >= expiryMillis)
            // Authentic, and past its deadline. Nothing is decoded here: the payload of an expired token is
            // not returned to anyone, so the only thing that escapes this branch is the fact of staleness.
            return Checked.EXPIRED;

        try {
            return new Checked(Validity.VALID,
                new String(DECODER.decode(signedPart.substring(0, expirySeparator)), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return Checked.INVALID;
        }
    }

    private static byte[] mac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // A missing HmacSHA256 or a rejected key is a configuration fault, not a bad token. Throwing
            // here would let a caller treat it as "invalid token" and carry on unauthenticated.
            throw new IllegalStateException("Cannot compute HMAC", e);
        }
    }
}
