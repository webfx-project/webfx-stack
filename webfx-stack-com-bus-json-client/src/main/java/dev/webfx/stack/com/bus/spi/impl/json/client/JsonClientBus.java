package dev.webfx.stack.com.bus.spi.impl.json.client;

import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Handler;
import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.com.bus.Message;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import dev.webfx.stack.session.state.client.ClientSideStateSessionSyncer;

/**
 * @author Bruno Salmon
 */
public abstract class JsonClientBus extends JsonBus {

    public JsonClientBus() {
    }

    public JsonClientBus(boolean alreadyOpen) {
        super(alreadyOpen);
    }


    @Override
    protected void onOpen() {
        super.onOpen();
        ClientSideStateSessionSyncer.setClientConnected(true);
    }

    @Override
    protected void onClose(Object reason) {
        super.onClose(reason);
        ClientSideStateSessionSyncer.setClientConnected(false);
    }

    @Override
    protected void onError(Object reason) {
        super.onError(reason);
    }

    @Override
    protected boolean onMessage(Message message) {
        if (message.options().isLocalOnly())
            return super.onMessage(message);
        Object incomingState = message.state(); // incoming client state coming from the server
        // We eventually enrich the incoming state with information from the client session, and/or update the client
        // session with new information coming from the server, with syncIncomingState()
        incomingState = ClientSideStateSessionSyncer.syncIncomingState(incomingState);
        // We publish the message with the incoming state in the thread local state holder (message subscribers can
        // access the incoming state with ThreadLocalStateHolder.getState())
        return ThreadLocalStateHolder.runWithState(incomingState, () -> super.onMessage(message));
    }

    @Override
    protected <T> void sendOrPublishOverNetwork(boolean send, String address, Object body, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) {
        Object outgoingState = options.getState(); // outgoing client state going to the server
        // We eventually enrich this state with information stored from the client session when it makes sense (ex:
        // userId, serverSessionId, runId, backoffice...), and update the client session
        outgoingState = ClientSideStateSessionSyncer.syncOutgoingState(outgoingState);
        // We pass this state in the delivery options
        options.setState(outgoingState);
        // And finally publish the message
        super.sendOrPublishOverNetwork(send, address, body, options, replyHandler);
    }

}
