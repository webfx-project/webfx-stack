package dev.webfx.stack.webpush.buscall;

/**
 * Request payload for the {@code SendPushNotification} BO operation.
 *
 * @param target          Opaque host-defined object identifying the recipient
 *                        set. Deserialised via the {@code $codec} discriminator
 *                        so each host application can plug in its own typed
 *                        target shape (KBS uses
 *                        {@code one.modality.crm.server.webpush.ModalityWebPushTarget}
 *                        with event/document/organization fields). webfx-stack
 *                        passes this through verbatim to the
 *                        {@link dev.webfx.stack.webpush.spi.WebPushSubscriptionStore}
 *                        implementation, which is the only thing that
 *                        interprets it.
 * @param title           Notification title shown in the system tray.
 * @param body            Notification body text.
 * @param url             Optional deep-link URL the notification opens when
 *                        clicked. Null means the SW falls back to the app root.
 * @param testSendToSelf  When true, the server narrows the recipient set to
 *                        subscriptions whose recipient email matches the
 *                        currently authenticated user — useful to preview the
 *                        message on the operator's own devices before broadcasting.
 *
 * @author Bruno Salmon
 */
public record SendPushNotificationArgument(
        Object target,
        String title,
        String body,
        String url,
        boolean testSendToSelf
) { }
