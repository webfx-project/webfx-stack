package dev.webfx.stack.webpush.buscall;

/**
 * Result of an {@code UnsubscribePushNotifications} call.
 *
 * @param email     The email that was actually unsubscribed (after auth-override).
 *                  Echoed back so the FO page can confirm "unsubscribed
 *                  {email}" without re-fetching auth state.
 * @param disabled  Number of recipient rows newly marked unsubscribed. Zero
 *                  means the email had no active opt-ins (already
 *                  unsubscribed, never subscribed, or different email cased
 *                  differently — the store-level match is case-insensitive).
 *
 * @author Bruno Salmon
 */
public record UnsubscribePushNotificationsResult(
        String email,
        int disabled
) { }
