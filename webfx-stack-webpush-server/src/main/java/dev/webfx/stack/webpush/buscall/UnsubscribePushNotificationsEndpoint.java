package dev.webfx.stack.webpush.buscall;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * Bus endpoint registering the {@code UnsubscribePushNotifications} operation
 * at {@link WebPushBusAddress#UNSUBSCRIBE_PUSH_NOTIFICATIONS_ADDRESS}.
 * Provided via {@code module-info.java}'s {@code provides BusCallEndpoint}
 * so it's auto-registered during application boot.
 *
 * <p><b>Auth model</b>: the endpoint accepts both authenticated and anonymous
 * calls (anonymous is needed for the operator-shared {@code /unsubscribe?email=…}
 * link to work without a login). When authenticated, the executor narrows the
 * scope to the caller's own email — see {@link UnsubscribePushNotificationsExecutor}.
 *
 * @author Bruno Salmon
 */
public final class UnsubscribePushNotificationsEndpoint
        extends AsyncFunctionBusCallEndpoint<UnsubscribePushNotificationsArgument, UnsubscribePushNotificationsResult> {

    public UnsubscribePushNotificationsEndpoint() {
        super(WebPushBusAddress.UNSUBSCRIBE_PUSH_NOTIFICATIONS_ADDRESS,
              UnsubscribePushNotificationsExecutor::execute);
    }
}
