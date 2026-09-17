package dev.webfx.stack.webpush.buscall;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * Bus endpoint registering the {@code SendPushNotification} operation at
 * {@link WebPushBusAddress#SEND_PUSH_NOTIFICATION_ADDRESS}. Provided via
 * {@code module-info.java}'s {@code provides BusCallEndpoint} so it's
 * auto-registered during application boot.
 *
 * @author Bruno Salmon
 */
public final class SendPushNotificationEndpoint
        extends AsyncFunctionBusCallEndpoint<SendPushNotificationArgument, SendPushNotificationResult> {

    public SendPushNotificationEndpoint() {
        super(WebPushBusAddress.SEND_PUSH_NOTIFICATION_ADDRESS,
              SendPushNotificationExecutor::execute);
    }
}
