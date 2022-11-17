package dev.webfx.stack.com.bus.spi.impl.json.client;

import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Handler;
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
        if (message.isLocal())
            return super.onMessage(message);
        // If the incoming message comes from the server, we update the client holders from it
        Object serverState = message.state();
        // We update the client session from the server state if necessary
        ClientSideStateSessionSyncer.syncClientSessionFromIncomingServerState(serverState);
        // We eventually enrich the server state with information from the client session
        serverState = ClientSideStateSessionSyncer.syncIncomingServerStateFromClientSession(serverState);
        try (ThreadLocalStateHolder ignored = ThreadLocalStateHolder.open(serverState)) {
            return super.onMessage(message);
        }
    }

    @Override
    protected <T> void sendOrPublishOverNetwork(boolean send, String address, Object body, Object state, Handler<AsyncResult<Message<T>>> replyHandler) {
        // Completing the state before sending it to the server
        state = ClientSideStateSessionSyncer.syncOutgoingClientStateFromClientSession(state);
        super.sendOrPublishOverNetwork(send, address, body, state, replyHandler);
        ClientSideStateSessionSyncer.syncClientSessionFromOutgoingClientState(state);
    }

/*
    @Override
    protected String jsonToNetworkRawMessage(WritableJsonObject jsonRawMessage, Object state) {
        // Completing the state before sending it to the server
        state = ClientSideStateSessionSyncer.syncOutgoingClientStateFromClientSession(state);
        ClientSideStateSessionSyncer.syncClientSessionFromOutgoingClientState(state);
        return super.jsonToNetworkRawMessage(jsonRawMessage, state);
    }
*/
}
