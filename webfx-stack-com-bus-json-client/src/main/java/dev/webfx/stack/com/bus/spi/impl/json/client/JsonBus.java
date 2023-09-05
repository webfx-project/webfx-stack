/*
 * Note: this code is a fork of Goodow realtime-channel project https://github.com/goodow/realtime-channel
 */

/*
 * Copyright 2013 Goodow.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package dev.webfx.stack.com.bus.spi.impl.json.client;

import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.ast.json.JsonObject;
import dev.webfx.platform.ast.json.ReadOnlyJsonObject;
import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.com.bus.Message;
import dev.webfx.stack.com.bus.spi.impl.client.NetworkBus;
import dev.webfx.stack.com.bus.spi.impl.json.JsonBusConstants;
import dev.webfx.stack.session.state.StateAccessor;

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
    protected Message<?> parseIncomingNetworkRawMessage(String rawMessage) {
        JsonObject jsonRawMessage = parseJsonRawMessage(rawMessage);
        ReadOnlyJsonObject headers = jsonRawMessage.getObject(HEADERS);
        Object state = headers == null ? null : StateAccessor.decodeState(headers.getString(HEADERS_STATE));
        return parseIncomingNetworkRawMessage(jsonRawMessage.getString(ADDRESS), jsonRawMessage.getString(REPLY_ADDRESS), jsonRawMessage.get(BODY), new DeliveryOptions().setState(state));
    }

    protected JsonObject parseJsonRawMessage(String rawMessage) {
        return Json.parseObject(rawMessage);
    }

    @Override
    protected String createOutgoingNetworkRawMessage(boolean send, String address, Object body, DeliveryOptions options, String replyAddress) {
        // We first create its Json raw representation.
        JsonObject jsonRawMessage = Json.createObject()
                .set(TYPE, send ? SEND : PUBLISH)
                .set(ADDRESS, address)
                .set(BODY, body);
        // We add the reply address if set
        if (replyAddress != null)
            jsonRawMessage.set(REPLY_ADDRESS, replyAddress);
        return jsonToNetworkRawMessage(jsonRawMessage, options);
    }

    protected String jsonToNetworkRawMessage(JsonObject jsonRawMessage) {
        return jsonToNetworkRawMessage(jsonRawMessage, new DeliveryOptions());
    }

    protected String jsonToNetworkRawMessage(JsonObject jsonRawMessage, DeliveryOptions options) {
        // If there is a state to transmit, we encode it and put it in the message headers
        setJsonRawMessageState(jsonRawMessage, options);
        return jsonRawMessage.toJsonString();
    }

    protected void setJsonRawMessageState(JsonObject jsonRawMessage, DeliveryOptions options) {
        Object state = options.getState();
        if (state != null)
            jsonRawMessage.set(HEADERS, Json.createObject()
                    .set(HEADERS_STATE, StateAccessor.encodeState(state)));
    }

    @Override
    protected String createRegisterNetworkRawMessage(String address) {
        return jsonToNetworkRawMessage(Json.createObject()
                .set(TYPE, REGISTER)
                .set(ADDRESS, address)
        );
    }

    @Override
   protected String createUnregisterNetworkRawMessage(String address) {
        return jsonToNetworkRawMessage(Json.createObject()
                .set(TYPE, UNREGISTER)
                .set(ADDRESS, address)
        );
    }

    protected void sendPing() {
        sendOutgoingNetworkRawMessage(createPingNetworkRawMessage());
    }

    protected String createPingNetworkRawMessage() {
        return jsonToNetworkRawMessage(Json.createObject()
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