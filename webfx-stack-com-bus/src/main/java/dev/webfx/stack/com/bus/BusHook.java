package dev.webfx.stack.com.bus;

import dev.webfx.platform.async.Handler;
import dev.webfx.platform.async.AsyncResult;

/**
 * A hook that you can use to receive various events on the Bus.
 *
 * @author Bruno Salmon
 */
public interface BusHook {
    /**
     * Called when the bus is opened
     */
    default void handleOpened() {}

    /**
     * Called when the bus is closed
     */
    default void handlePostClose() {}

    /**
     * Called before close the bus
     *
     * @return true to close the bus, false to reject it
     */
    default boolean handlePreClose() { return true; }

    /**
     * Called before register a handler
     *
     * @param address The address
     * @param handler The handler
     * @return true to let the registration occur, false otherwise
     */
    @SuppressWarnings("rawtypes")
    default boolean handlePreRegister(String address, Handler<? extends Message> handler) { return true; }

    /**
     * Called when a message is received
     *
     * @param message The message
     * @return true To allow the message to deliver, false otherwise
     */
    default boolean handleReceiveMessage(Message<?> message) { return true; }

    /**
     * Called when sending or publishing on the bus
     *
     * @param send         if true it's a send else it's a publish
     * @param address      The address the message is being sent/published to
     * @param msg          The message
     * @param replyHandler Reply handler will be called when any reply from the recipient is received
     * @return true To allow the send/publish to occur, false otherwise
     */
    default <T> boolean handleSendOrPub(boolean send, String address, Object msg, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) {
        return true;
    }

    /**
     * Called when unregistering a handler
     *
     * @param address The address
     * @return true to let the unregistration occur, false otherwise
     */
    default boolean handleUnregister(String address) { return true; }
}
