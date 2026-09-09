package dev.webfx.stack.session.token;

/**
 * What a token turned out to say, once this server had proved it wrote it.
 *
 * <p>Only ever constructed after the signature holds, so every field here is something this server
 * asserted rather than something a caller supplied. That is the difference between this and the userId
 * beside it in the same message.
 *
 * @param principal          the identity a credential check established, at the moment it was established
 * @param familyId           the session family this token belongs to, or null for a token minted before
 *                           families existed, or by a server with no store to keep one in
 * @param generation         which link in the family's renewal chain this is; meaningless without a familyId
 * @param accessExpiryMillis when this token stops being usable without renewal
 * @param sessionEndMillis   the signed deadline: the point past which the token stops verifying at all,
 *                           and with it the session
 * @param absoluteExpiryMillis the outer bound renewal may never push past
 * @param tier               which lifetime policy this session was opened under, or null for a legacy token
 *
 * @author Claude Code
 */
public record IdentityToken(
    Object principal,
    String familyId,
    int generation,
    long accessExpiryMillis,
    long sessionEndMillis,
    long absoluteExpiryMillis,
    SessionTier tier) {

    /** Whether this token may be acted on as it stands, with no renewal. */
    public boolean isWithinAccessWindow(long nowMillis) {
        return nowMillis < accessExpiryMillis;
    }

    /**
     * Whether the token is close enough to the end of its access window to be worth exchanging now.
     *
     * <p>True for a token whose window has already passed, too: such a message cannot proceed until the
     * exchange has happened, so both cases go the same way and only the caller's willingness to wait
     * differs.
     */
    public boolean isRenewalDue(long nowMillis) {
        return SessionLifetime.isRenewalDue(nowMillis, accessExpiryMillis);
    }

    /**
     * A token minted before this mechanism existed: one fixed lifetime, no family, no tier.
     *
     * <p>They keep working exactly as they did, and are upgraded in place at their first renewal rather
     * than being refused — an upgrade extends a proof that was already made, which is not the same act
     * as manufacturing one from a claim. Refusing them instead would sign out everyone holding one at
     * the moment of deploy, for no gain: they are this server's own signature either way.
     */
    public boolean isLegacy() {
        return tier == null;
    }
}
