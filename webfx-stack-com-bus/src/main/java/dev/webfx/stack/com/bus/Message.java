package dev.webfx.stack.com.bus;

import dev.webfx.platform.async.Handler;
import dev.webfx.platform.async.AsyncResult;

/**
 * Represents a message on the event bus.
 *
 * @author Bruno Salmon
 */

public interface Message<T> {
    /**
     * The body of the message
     */
    T body();

    /**
     * Signal that processing of this message failed. If the message was sent specifying a result handler,
     * the handler will be called with a failure corresponding to the failure code and message
     * specified here
     *
     * @param failureCode A failure code to pass back to the sender
     * @param msg         A message to pass back to the sender
     */
    void fail(int failureCode, String msg);

    /**
     * Reply to this message. If the message was sent specifying a reply handler, that handler will be
     * called when it has received a reply. If the message wasn't sent specifying a receipt handler,
     * this method does nothing.
     */
    void reply(Object body, DeliveryOptions options);

    /**
     * The same as {@code reply(Object body)} but you can specify handler for the reply - i.e., to
     * receive the reply to the reply.
     */
    @SuppressWarnings("hiding")
    <T> void reply(Object body, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler);

    /**
     * The reply address (if any)
     */
    String replyAddress();

    /**
     * The address the message was sent to
     */
    String address();

    DeliveryOptions options();

    default Object state() {
        return options().getState();
    }
}
