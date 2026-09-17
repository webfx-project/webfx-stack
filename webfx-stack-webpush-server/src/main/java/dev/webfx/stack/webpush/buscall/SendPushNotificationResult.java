package dev.webfx.stack.webpush.buscall;

/**
 * Aggregate outcome of a {@code SendPushNotification} call. Counts are
 * mutually exclusive: each targeted subscription contributes to exactly one
 * bucket. {@code targeted = succeeded + expired + failed}.
 *
 * @param targeted  Total subscriptions the operation tried to push to.
 * @param succeeded 2xx response from the push service — message accepted.
 * @param expired   Subscription returned 404/410 — the device row was marked
 *                  dead on the server side and won't receive further pushes.
 * @param failed    Anything else (5xx, network errors, signing failures).
 *                  Often transient; the operator can retry.
 *
 * @author Bruno Salmon
 */
public record SendPushNotificationResult(
        int targeted,
        int succeeded,
        int expired,
        int failed
) { }
