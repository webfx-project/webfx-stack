package dev.webfx.stack.com.bus.spi.impl.json.client;

import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Handler;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.com.bus.Message;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import dev.webfx.stack.session.state.client.ClientSideStateSessionSyncer;

/**
 * @author Bruno Salmon
 */
public abstract class JsonClientBus extends JsonBus {

    private static final boolean LOG_STATES = false;

    public JsonClientBus() {
    }

    public JsonClientBus(boolean alreadyOpen) {
        super(alreadyOpen);
    }


    @Override
    protected void onOpen() {
        ClientSideStateSessionSyncer.setClientConnected(true);
        super.onOpen();
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
        // If the incoming message comes from the server, we update the client holders from it
        Object state = message.state();
        Object incomingStateCapture = LOG_STATES ? "" + state : null;
        // We update the client session from the server state if necessary
        ClientSideStateSessionSyncer.syncClientSessionFromIncomingServerState(state);
        // We eventually enrich the server state with information from the client session
        state = ClientSideStateSessionSyncer.syncIncomingServerStateFromClientSession(state);
        if (LOG_STATES)
            Console.log("<< incoming sate: " + state + " << " + incomingStateCapture);
        return ThreadLocalStateHolder.runWithState(state, () -> super.onMessage(message));
    }

    @Override
    protected <T> void sendOrPublishOverNetwork(boolean send, String address, Object body, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) {
        // Completing the state before sending it to the server
        Object state = options.getState();
        Object incomingStateCapture = LOG_STATES ? "" + state : null;
        state = ClientSideStateSessionSyncer.syncOutgoingClientStateFromClientSession(state);
        if (LOG_STATES)
            Console.log(">> outgoing sate: " + incomingStateCapture + " >> " + state);
        options.setState(state);
        super.sendOrPublishOverNetwork(send, address, body, options, replyHandler);
        ClientSideStateSessionSyncer.syncClientSessionFromOutgoingClientState(state);
    }

}
