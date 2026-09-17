package dev.webfx.stack.webpush;

/**
 * Outcome of a single {@link WebPushServerService#send} call. Sealed so callers can
 * exhaustively switch over the three semantically distinct cases:
 * <ul>
 *   <li>{@link Success} — push service accepted the message (2xx).</li>
 *   <li>{@link SubscriptionExpired} — push service returned 410 Gone or 404 Not Found.
 *       The subscription is permanently dead; the caller should mark the row dead in
 *       its database so future sends to it are skipped.</li>
 *   <li>{@link Failed} — anything else (4xx other than 410/404, 5xx, network errors).
 *       Often transient — the caller may retry with backoff.</li>
 * </ul>
 *
 * @author Bruno Salmon
 */
public sealed interface WebPushResult {

    /** Push service accepted the message. {@code statusCode} is typically 201 Created. */
    record Success(int statusCode) implements WebPushResult {
    }

    /**
     * Push service reports the subscription is gone for good. Storage should
     * mark this subscription dead to avoid wasting future sends on it.
     */
    record SubscriptionExpired(int statusCode) implements WebPushResult {
    }

    /**
     * Some other failure. {@code statusCode} is 0 for network-level failures
     * (no response received) and the actual HTTP status for service errors.
     * {@code message} contains a short description suitable for logging.
     */
    record Failed(int statusCode, String message) implements WebPushResult {
    }
}
