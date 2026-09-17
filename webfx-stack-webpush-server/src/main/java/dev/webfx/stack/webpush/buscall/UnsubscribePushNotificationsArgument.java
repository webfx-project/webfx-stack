package dev.webfx.stack.webpush.buscall;

/**
 * Request payload for the {@code UnsubscribePushNotifications} FO operation.
 *
 * @param email Email of the account to unsubscribe. <b>Ignored when the
 *              caller is authenticated</b> — the executor uses the auth-claim
 *              email instead, so a logged-in user can only unsubscribe
 *              themselves. Required for anonymous unsubscribe (operator-shared
 *              {@code /unsubscribe?email=…} link).
 *
 * @author Bruno Salmon
 */
public record UnsubscribePushNotificationsArgument(
        String email
) { }
