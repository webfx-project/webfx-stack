package dev.webfx.stack.authn;

/**
 * Credentials that instruct the server to <b>create</b> a BOOKING_ACCESS magic
 * link for the given email + booking and <b>return its 6-digit verification
 * code</b> to the caller — without sending any email.
 *
 * <p>Designed for the post-booking confirmation flow where the user is asked
 * to install the PWA. The success page displays the returned code prominently
 * so the user can copy it before leaving Safari; the freshly-installed PWA
 * then accepts the code as a one-tap login that lands them at the booking
 * page (the code embeds the per-booking {@code requestedPath} that drives
 * the post-login redirect).
 *
 * <p>Inherits from {@link AlternativeLoginActionCredentials} so it serialises
 * over the existing wire shape and integrates with gateway dispatch
 * uniformly with {@link SendMagicLinkCredentials}; the gateway distinguishes
 * the two via {@code instanceof} and skips the mail-send branch for this
 * type. The base class fields used here:
 * <ul>
 *   <li>{@code email} — booking email, identifies the magic link</li>
 *   <li>{@code clientOrigin} — used to assemble the magic link URL for
 *       completeness, even though no email is sent containing it</li>
 *   <li>{@code requestedPath} — where the PWA redirects after redeem
 *       (typically {@code /orders/{documentId}})</li>
 *   <li>{@code language} — persisted on the magic link for future reference</li>
 *   <li>{@code verificationCodeOnly}, {@code context} — currently unused by
 *       this flow; kept for parity with the base shape</li>
 * </ul>
 *
 * @author Bruno Salmon
 */
public final class IssueBookingAccessMagicLinkCredentials extends AlternativeLoginActionCredentials {

    public IssueBookingAccessMagicLinkCredentials(String email, String clientOrigin, String requestedPath, Object language, boolean verificationCodeOnly, Object context) {
        super(email, clientOrigin, requestedPath, language, verificationCodeOnly, context);
    }
}
