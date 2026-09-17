package dev.webfx.stack.webpush;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.webpush.spi.WebPushServerServiceProvider;

import java.util.ServiceLoader;

/**
 * Static facade for sending Web Push messages (W3C / RFC 8030) to browser
 * subscriptions. Mirrors the {@code XxxServerService} pattern used elsewhere
 * in webfx-stack (see {@code PushServerService}, {@code QueryPushServerService},
 * {@code AuthorizationServerService}).
 * <p>
 * Naming: there is no client-side counterpart today — a Web Push CLIENT
 * library would be the browser itself, not a Java module — but the
 * "ServerService" suffix is kept for consistency with the conventional
 * naming. If a Java-side client SDK ever appears (e.g. for a desktop admin
 * tool that sends pushes directly), no rename is needed here.
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * WebPushSubscription sub = ...;       // loaded from DB
 * WebPushPayload payload = new WebPushPayload(
 *     "Talk reminder",
 *     "Your talk starts in 1 hour",
 *     "/my-bookings/" + ref,
 *     "talk-reminder-" + ref,
 *     86400,                            // 24h TTL
 *     WebPushPayload.Urgency.NORMAL);
 *
 * WebPushServerService.send(sub, payload)
 *     .onSuccess(result -> {
 *         switch (result) {
 *             case WebPushResult.Success ok           -> log.info("sent: {}", ok.statusCode());
 *             case WebPushResult.SubscriptionExpired e -> markDead(sub);
 *             case WebPushResult.Failed f             -> retryLater(sub, payload);
 *         }
 *     });
 * }</pre>
 *
 * @author Bruno Salmon
 */
public final class WebPushServerService {

    private WebPushServerService() {
        // Static facade only — never instantiated.
    }

    /** Returns the SPI provider. Callers normally use {@link #send} directly. */
    public static WebPushServerServiceProvider getProvider() {
        // SPI class extracted to a local — referencing it twice inline causes
        // webfx-cli to emit duplicate `uses` directives in module-info.java.
        Class<WebPushServerServiceProvider> spi = WebPushServerServiceProvider.class;
        return SingleServiceProvider.getProvider(spi, () -> ServiceLoader.load(spi));
    }

    /**
     * Encrypts {@code payload} for {@code subscription} and POSTs the result
     * to the push service. See {@link WebPushServerServiceProvider#send} for
     * the full result-type semantics.
     */
    public static Future<WebPushResult> send(WebPushSubscription subscription, WebPushPayload payload) {
        return getProvider().send(subscription, payload);
    }

    /**
     * The server's current VAPID public key.
     * See {@link WebPushServerServiceProvider#currentVapidPublicKey}.
     */
    public static String currentVapidPublicKey() {
        return getProvider().currentVapidPublicKey();
    }
}
