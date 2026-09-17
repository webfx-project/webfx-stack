package dev.webfx.stack.webpush.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.webpush.WebPushPayload;
import dev.webfx.stack.webpush.WebPushResult;
import dev.webfx.stack.webpush.WebPushSubscription;

/**
 * SPI for the Web Push send operation. Implementations are discovered via
 * {@link java.util.ServiceLoader} during application boot; the platform's
 * default provider is registered by this module's {@code WebPushServerModuleBooter}
 * after reading the VAPID configuration.
 * <p>
 * The static facade {@link dev.webfx.stack.webpush.WebPushServerService} is
 * the API consumers should call — they shouldn't need to look up the
 * provider directly.
 *
 * @author Bruno Salmon
 */
public interface WebPushServerServiceProvider {

    /**
     * Encrypts {@code payload} for {@code subscription} and POSTs the result
     * to the push service indicated by {@code subscription.endpoint()}.
     *
     * @return a Future resolving to the outcome. Never fails outright — network
     *     and HTTP errors are surfaced as {@link WebPushResult.Failed} so
     *     callers don't need defensive try/catch around the call.
     */
    Future<WebPushResult> send(WebPushSubscription subscription, WebPushPayload payload);

    /**
     * The server's current VAPID public key — what the application would hand
     * to a fresh client subscribe so its {@code applicationServerKey} matches.
     * Used by send-time filters to skip subscriptions whose stored key no
     * longer matches (env crossover after a DB copy, or post-key-rotation
     * orphans).
     */
    String currentVapidPublicKey();
}
