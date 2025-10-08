package dev.webfx.stack.com.bus.spi.impl.json.client;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.com.bus.Message;
import dev.webfx.stack.com.bus.spi.impl.client.NetworkBus;
import dev.webfx.stack.com.bus.spi.impl.json.JsonBusConstants;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.client.ClientSideStateSessionSyncer;

/**
 * @author Bruno Salmon
 */
public abstract class JsonBus extends NetworkBus implements JsonBusConstants {

    public JsonBus() {
    }

    public JsonBus(boolean alreadyOpen) {
        super(alreadyOpen);
    }

    @Override
    protected boolean isPingRawMessage(String rawMessage) {
        return "{\"type\":\"ping\"}".equals(rawMessage);
    }

    @Override
    protected boolean isPongRawMessage(String rawMessage) {
        return "{\"type\":\"pong\"}".equals(rawMessage);
    }

    protected boolean isPongMessage(ReadOnlyAstObject jsonRawMessage) {
        return "pong".equals(jsonRawMessage.getString("type"));
    }

    @Override
    protected Message<?> parseIncomingNetworkRawMessage(String rawMessage) {
        if (isPongRawMessage(rawMessage))
            return null;
        ReadOnlyAstObject jsonRawMessage = parseJsonRawMessage(rawMessage);
        ReadOnlyAstObject headers = jsonRawMessage.getObject(HEADERS);
        Object state = headers == null ? null : StateAccessor.decodeState(headers.getString(HEADERS_STATE));
        String address = jsonRawMessage.getString(ADDRESS);
        if (address != null)
            return parseIncomingNetworkRawMessage(address, jsonRawMessage.getString(REPLY_ADDRESS), jsonRawMessage.get(BODY), new DeliveryOptions().setState(state));
        // Particular case of a pong with a state. This happens when the server received a ping but couldn't retrieve
        // the previous session (ex: on server restart). This special pong is passing the new server session id, and
        // is also a request to send the whole client state back again to the server. Because, without knowing its runId,
        // the server can't make push notifications anymore to that client.
        if (isPongMessage(jsonRawMessage)) {
            // We ask ClientSideStateSessionSyncer to consider this incoming state. It will mark all client states as
            // to be sent to the server again on the next client call.
            ClientSideStateSessionSyncer.syncIncomingState(state);
            // We don't wait for the next possible server call caused by a user interaction but make one right now
            sendPingStateNow(); // The whole client state should be sent with that ping.
        }
        return null;
    }

    protected ReadOnlyAstObject parseJsonRawMessage(String rawMessage) {
        return Json.parseObject(rawMessage);
    }

    @Override
    protected String createOutgoingNetworkRawMessage(boolean send, String address, Object body, DeliveryOptions options, String replyAddress) {
        // We first create its JSON raw representation.
        AstObject jsonRawMessage = AST.createObject()
                .set(TYPE, send ? SEND : PUBLISH)
                .set(ADDRESS, address)
                .set(BODY, body);
        // We add the reply address if set
        if (replyAddress != null)
            jsonRawMessage.set(REPLY_ADDRESS, replyAddress);
        return jsonToNetworkRawMessage(jsonRawMessage, options);
    }

    protected String jsonToNetworkRawMessage(AstObject jsonRawMessage) {
        return jsonToNetworkRawMessage(jsonRawMessage, new DeliveryOptions());
    }

    protected String jsonToNetworkRawMessage(AstObject jsonRawMessage, DeliveryOptions options) {
        // If there is a state to transmit, we encode it and put it in the message headers
        setJsonRawMessageState(jsonRawMessage, options);
        return Json.formatNode(jsonRawMessage);
    }

    protected void setJsonRawMessageState(AstObject jsonRawMessage, DeliveryOptions options) {
        Object state = options.getState();
        if (state != null) {
            AstObject headers = AST.createObject().set(HEADERS_STATE, StateAccessor.encodeState(state));
            jsonRawMessage.set(HEADERS, headers);
        }
    }

    @Override
    protected String createRegisterNetworkRawMessage(String address) {
        return jsonToNetworkRawMessage(AST.createObject()
                .set(TYPE, REGISTER)
                .set(ADDRESS, address)
        );
    }

    @Override
   protected String createUnregisterNetworkRawMessage(String address) {
        return jsonToNetworkRawMessage(AST.createObject()
                .set(TYPE, UNREGISTER)
                .set(ADDRESS, address)
        );
    }

    protected void sendPing() {
        sendOutgoingNetworkRawMessage(createPingNetworkRawMessage());
    }

    protected String createPingNetworkRawMessage() {
        return jsonToNetworkRawMessage(AST.createObject()
                .set(TYPE, PING)
        );
    }

    protected void sendPingState() {
        if (JsonClientBusModuleBooter.isCommunicationAllowed())
            sendPingStateNow();
        else
            JsonClientBusModuleBooter.registerPendingPingStateJsonBus(this);
    }

    void sendPingStateNow() {
        sendOrPublishOverNetwork(true, PING_STATE_ADDRESS, null, new DeliveryOptions(), event -> System.out.println("Server acknowledged ping state"));
    }

}