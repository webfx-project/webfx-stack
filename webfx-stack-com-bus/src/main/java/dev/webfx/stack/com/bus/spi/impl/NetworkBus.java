package dev.webfx.stack.com.bus.spi.impl;

import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Handler;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.com.bus.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public abstract class NetworkBus extends SimpleBus {

    // TODO: remove that public visibility
    public final Map<String, Integer> handlerCount = new HashMap<>();

    public NetworkBus() {
    }

    public NetworkBus(boolean alreadyOpen) {
        super(alreadyOpen);
    }

    @Override
    protected void onClose(Object reason) {
        super.onClose(reason);
        handlerCount.clear();
    }

    protected abstract Message<?> parseIncomingNetworkRawMessage(String rawMessage);

    protected abstract void sendOutgoingNetworkRawMessage(String rawMessage);

    protected void onIncomingNetworkRawMessage(String rawMessage) {
        Console.log("Received incoming network raw message: " + rawMessage);
        Message<?> parsedMessage = parseIncomingNetworkRawMessage(rawMessage);
        onMessage(parsedMessage);
    }

    protected Message<?> parseIncomingNetworkRawMessage(String address, String replyAddress, Object body, Object state) {
        return createMessage(false, false, address, replyAddress, body, state);
    }

    @Override
    protected <T> void doSendOrPublishImpl(boolean local, boolean send, String address, Object body, Object state, Handler<AsyncResult<Message<T>>> replyHandler) {
        // If it's a local message, we can use the default implementation
        if (local)
            super.doSendOrPublishImpl(true, send, address, body, state, replyHandler);
        else
            sendOrPublishOverNetwork(send, address, body, state, replyHandler);
    }

    protected <T> void sendOrPublishOverNetwork(boolean send, String address, Object body, Object state, Handler<AsyncResult<Message<T>>> replyHandler) {
        // Registering the reply handler (if set)
        String replyAddress = registerReplyHandlerIfSet(replyHandler);
        // Creating the network raw message
        String rawMessage = createOutgoingNetworkRawMessage(send, address, body, state, replyAddress);
        sendOutgoingNetworkRawMessage(rawMessage);
    }

    protected abstract String createOutgoingNetworkRawMessage(boolean send, String address, Object body, Object state, String replyAddress);

    @Override
    protected boolean doRegister(boolean local, String address, Handler<? extends Message> handler) {
        // Trying to register with the default implementation
        boolean registered = super.doRegister(local, address, handler);
        if (local || !registered)
            return false;
        if (handlerCount.containsKey(address)) {
            handlerCount.put(address, handlerCount.get(address) + 1);
            return false;
        }
        handlerCount.put(address, 1);
        sendRegister(address);
        return true;
    }

    @Override
    protected <T> boolean doUnregister(boolean local, String address, Handler<Message<T>> handler) {
        boolean unsubscribed = super.doUnregister(local, address, handler);
        if (local || !unsubscribed || (hook != null && !hook.handleUnregister(address)))
            return false;
        handlerCount.put(address, handlerCount.get(address) - 1);
        if (handlerCount.get(address) == 0) {
            handlerCount.remove(address);
            sendUnregister(address);
            return true;
        }
        return false;
    }


    protected void sendRegister(String address) {
        sendOutgoingNetworkRawMessage(createRegisterNetworkRawMessage(address));
    }

    protected abstract String createRegisterNetworkRawMessage(String address);

    /*
     * No more handlers so we should unregister the connection
     */
    protected void sendUnregister(String address) {
        sendOutgoingNetworkRawMessage(createUnregisterNetworkRawMessage(address));
    }

    protected abstract String createUnregisterNetworkRawMessage(String address);

}
