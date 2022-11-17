package dev.webfx.stack.com.bus.spi.impl.json.server;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.WritableJsonObject;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.spi.impl.json.JsonBusConstants;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.server.ServerSideStateSessionSyncer;


/**
 * @author Bruno Salmon
 */
public final class ServerJsonBusStateManager implements JsonBusConstants {

    private static final boolean LOG_RAW_MESSAGES = false;
    private static final boolean LOG_STATES = false;

    public static void initialiseStateManagement(Bus serverJsonBus) {
        // We register at PING_STATE_ADDRESS a handler that just replies with an empty body (but the states mechanism will automatically apply - which is the main purpose of that call)
        serverJsonBus.register(JsonBusConstants.PING_STATE_ADDRESS, event -> event.reply(null, null));
    }

    public static Future<Boolean> manageStateOnIncomingOrOutgoingRawJsonMessage(WritableJsonObject rawJsonMessage, Session serverSession, boolean fromClientToServer) {
        WritableJsonObject headers = rawJsonMessage.getObject(JsonBusConstants.HEADERS);
        Object state = headers == null ? null : StateAccessor.decodeState(headers.getString(JsonBusConstants.HEADERS_STATE));

        // Incoming message from client to server
        if (fromClientToServer) {
            if (LOG_RAW_MESSAGES)
                Console.log("Incoming message : " + rawJsonMessage.toJsonString());
            Future<Boolean> sessionStorageFuture = ServerSideStateSessionSyncer.syncServerSessionFromIncomingClientState(serverSession, state, serverSession.isEmpty());
            String stateFirst = LOG_STATES ? "" + state : null;
            state = ServerSideStateSessionSyncer.syncIncomingClientStateFromServerSession(state, serverSession);
            if (LOG_STATES)
                Console.log(">> incoming sate: " + stateFirst + " >> " + state);
            setJsonRawMessageState(rawJsonMessage, headers, state);
            return sessionStorageFuture;
        }

        // Outgoing message from server to client
        String stateFirst = LOG_STATES ? "" + state : null;
        state = ServerSideStateSessionSyncer.syncOutgoingServerStateFromServerSessionAndViceVersa(state, serverSession);
        if (LOG_STATES)
            Console.log("<< outgoing sate: " + state + " << " + stateFirst);
        setJsonRawMessageState(rawJsonMessage, headers, state);
        if (LOG_RAW_MESSAGES)
            Console.log("Outgoing message : " + rawJsonMessage.toJsonString());
        return Future.succeededFuture(null);
    }

    private static void setJsonRawMessageState(WritableJsonObject rawJsonMessage, WritableJsonObject headers, Object state) {
        if (state != null) {
            if (headers == null)
                rawJsonMessage.set(JsonBusConstants.HEADERS, headers = Json.createObject());
            headers.set(JsonBusConstants.HEADERS_STATE, StateAccessor.encodeState(state));
        }
    }
}
