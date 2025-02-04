package dev.webfx.stack.com.bus.spi.impl.json.server;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.com.bus.spi.impl.json.JsonBusConstants;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.server.ServerSideStateSessionSyncer;

import java.util.function.Consumer;


/**
 * @author Bruno Salmon
 */
public final class ServerJsonBusStateManager implements JsonBusConstants {

    private final static boolean LOG_RAW_MESSAGES = false;

    public static void initialiseStateManagement(Bus serverJsonBus) {
        // We register at PING_STATE_ADDRESS a handler that just replies with an empty body (but the states mechanism will automatically apply - which is the main purpose of that call)
        serverJsonBus.register(JsonBusConstants.PING_STATE_ADDRESS, message -> message.reply(null, new DeliveryOptions()));
    }

    public static Future<Session> manageStateOnIncomingOrOutgoingRawJsonMessage(AstObject rawJsonMessage, Session serverSession, boolean incoming) {
        AstObject headers = rawJsonMessage.getObject(JsonBusConstants.HEADERS);
        Object originalState = headers == null ? null : StateAccessor.decodeState(headers.getString(JsonBusConstants.HEADERS_STATE));

        // Incoming message (from client to server)
        if (incoming) {
            if (LOG_RAW_MESSAGES)
                Console.log(">> Incoming message : " + Json.formatNode(rawJsonMessage));
            // We sync the application serverSession with the incoming state. This is at this point that the serverSession
            // switch can happen if requested by the client, in which case a different serverSession will be returned.
            return ServerSideStateSessionSyncer.syncIncomingState(serverSession, originalState)
                    .compose(pair -> {
                        // Getting the final session and incoming state as a result
                        Session finalServerSession = pair.get1();
                        Object finalIncomingState = pair.get2();
                        // We memorise that final state in the raw message
                        setJsonRawMessageState(rawJsonMessage, headers, finalIncomingState);
                        // We tell the client is live
                        clientIsLive(finalIncomingState, finalServerSession);
                        // We tell the message delivery can now continue into the server, and return the serverSession (not
                        // sure if the serverSession object will be useful - most important thing is to complete this
                        // asynchronous operation so the delivery can go on)
                        return Future.succeededFuture(finalServerSession);
                    });
        }

        // Outgoing message (from server to client)
        // We enrich the state with possible further info coming from the serverSession (ex: serverSessionId change)
        Object finalOutgoingState = ServerSideStateSessionSyncer.syncOutgoingState(originalState, serverSession);
        // We pass that final state inside the raw message
        setJsonRawMessageState(rawJsonMessage, headers, finalOutgoingState);
        if (LOG_RAW_MESSAGES)
            Console.log("<< Outgoing message : " + Json.formatNode(rawJsonMessage));
        // We tell the message delivery can now continue into the client, and return the serverSession (not sure if the serverSession
        // object will be useful - most important thing is the to complete this asynchronous operation so the delivery can go on)
        return Future.succeededFuture(serverSession);
    }

    private static void setJsonRawMessageState(AstObject rawJsonMessage, AstObject headers, Object state) {
        if (state != null) {
            if (headers == null)
                rawJsonMessage.set(JsonBusConstants.HEADERS, headers = AST.createObject());
            headers.set(JsonBusConstants.HEADERS_STATE, StateAccessor.encodeState(state));
        }
    }

    private static Consumer<Object> clientLiveListener;

    public static void setClientLiveListener(Consumer<Object> clientLiveListener) {
        ServerJsonBusStateManager.clientLiveListener = clientLiveListener;
    }

    public static void clientIsLive(Object state, Session session) {
        if (clientLiveListener != null) {
            // Trying to get the client runId from the state
            String runId = StateAccessor.getRunId(state);
            if (runId == null) {
                // If not found, trying to get it from the session
                runId = SessionAccessor.getRunId(session);
            }
            if (runId != null)
                clientLiveListener.accept(runId);
            else
                Console.log("⚠️ ServerJsonBusStateManager.clientIsLive() was called but no runId could be found");
        }
    }

}
