package dev.webfx.stack.webpush;

/**
 * The application-level content to deliver as a push notification. Sent as JSON
 * to the browser; the service worker's {@code push} event handler reads it back.
 * <p>
 * Fields {@code title}, {@code body}, {@code url}, {@code tag} mirror the
 * conventions adopted by the front-end service worker (see {@code sw-core.ts}
 * in the React app). Anything else the application needs to attach to a
 * notification should be agreed between server and SW out-of-band.
 *
 * @param title        notification title (shown bold at the top)
 * @param body         notification body text (shown below the title)
 * @param url          target URL when the user taps the notification (passed through to the SW)
 * @param tag          optional collapsing key — a new notification with the same tag replaces the old one
 * @param ttlSeconds   how long the push service will hold the message if the device is offline (typical: a few hours to 1 day)
 * @param urgency      delivery urgency, controls push-service prioritisation
 * @author Bruno Salmon
 */
public record WebPushPayload(
        String title,
        String body,
        String url,
        String tag,
        int ttlSeconds,
        Urgency urgency
) {

    /**
     * Mirrors the W3C Push API urgency levels. Push services use this as a
     * delivery-prioritisation hint; for KBS use cases:
     * <ul>
     *   <li>{@link #VERY_LOW} — non-time-sensitive informational pushes (rare)</li>
     *   <li>{@link #LOW} — daily / weekly summaries</li>
     *   <li>{@link #NORMAL} — booking reminders (default)</li>
     *   <li>{@link #HIGH} — operational alerts that need immediate attention (rare)</li>
     * </ul>
     */
    public enum Urgency {
        VERY_LOW("very-low"),
        LOW("low"),
        NORMAL("normal"),
        HIGH("high");

        private final String wireValue;

        Urgency(String wireValue) {
            this.wireValue = wireValue;
        }

        /** Lowercase value sent in the {@code Urgency} HTTP header. */
        public String wireValue() {
            return wireValue;
        }
    }
}
