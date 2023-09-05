package dev.webfx.stack.com.bus.spi.impl.json.server;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.ast.json.JsonObject;
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
    private final static boolean LOG_STATES = false;
    private final static String ASSOCIATED_SESSION_KEY = "$associatedSession";

    public static void initialiseStateManagement(Bus serverJsonBus) {
        // We register at PING_STATE_ADDRESS a handler that just replies with an empty body (but the states mechanism will automatically apply - which is the main purpose of that call)
        serverJsonBus.register(JsonBusConstants.PING_STATE_ADDRESS, event -> event.reply(null, new DeliveryOptions()));
    }

    public static Future<Session> manageStateOnIncomingOrOutgoingRawJsonMessage(JsonObject rawJsonMessage, Session webServerSession, boolean incoming) {
        JsonObject headers = rawJsonMessage.getObject(JsonBusConstants.HEADERS);
        Object originalState = headers == null ? null : StateAccessor.decodeState(headers.getString(JsonBusConstants.HEADERS_STATE));
        String originalStateCapture = LOG_STATES ? "" + originalState : null;
        // Is there an application session already associated with the web session?
        Session associatedSession = webServerSession.get(ASSOCIATED_SESSION_KEY);
        // If yes, we use it as the application session, otherwise, we continue with the web session (but we may still
        // do a session switch if the client requests a specific serverSessionId)
        Session applicationServerSession = associatedSession != null ? associatedSession : webServerSession;

        // Incoming message (from client to server)
        if (incoming) {
            if (LOG_RAW_MESSAGES)
                Console.log(">> Incoming message : " + rawJsonMessage.toJsonString());
            Promise<Session> promise = Promise.promise();
            // We sync the application session with the incoming state. This is at this point that the session switch
            // can happen if requested by the client, in which case a different session will be returned.
            ServerSideStateSessionSyncer.syncServerSessionFromIncomingClientState(applicationServerSession, originalState)
                    .onComplete(ar -> {
                        // Getting the requested session (might be null if cleared by the server, in which case we continue with the previous one)
                        Session requestedSession = ar.result() != null ? ar.result() : applicationServerSession;
                        // If the session has been switched, we associate that new application session with the web
                        // session if not already done (for further calls)
                        if (requestedSession != applicationServerSession && associatedSession != requestedSession) {
                            //Console.log("Associating " + webServerSession.id() + " -> " + requestedSession.id());
                            webServerSession.put(ASSOCIATED_SESSION_KEY, requestedSession);
                        }
                        // Finally we complete the incoming state with possible further info coming from the session
                        Object finalState = ServerSideStateSessionSyncer.syncIncomingClientStateFromServerSession(originalState, requestedSession);
                        if (LOG_STATES)
                            Console.log(">> Incoming state: " + originalStateCapture + " >> " + finalState);
                        // We memorise that final state in the raw message
                        setJsonRawMessageState(rawJsonMessage, headers, finalState);
                        // We tell the client is live
                        clientIsLive(finalState, requestedSession);
                        // We tell the message delivery can now continue into the server, and return the session (not
                        // sure if the session object will be useful - most important thing is the to complete this
                        // asynchronous operation so the delivery can go on)
                        promise.complete(requestedSession);
                    });
            return promise.future();
        }

        // Outgoing message (from server to client)
        // We complete the state with possible further info coming from the session (ex: serverSessionId change)
        Object finalState = ServerSideStateSessionSyncer.syncOutgoingServerStateFromServerSessionAndViceVersa(originalState, applicationServerSession);
        if (LOG_STATES)
            Console.log("<< Outgoing state: " + finalState + " << " + originalStateCapture);
        // We memorise that final state in the raw message
        setJsonRawMessageState(rawJsonMessage, headers, finalState);
        if (LOG_RAW_MESSAGES)
            Console.log("<< Outgoing message : " + rawJsonMessage.toJsonString());
        // We tell the message delivery can now continue into the client, and return the session (not sure if the session
        // object will be useful - most important thing is the to complete this asynchronous operation so the delivery can go on)
        return Future.succeededFuture(applicationServerSession);
    }

    private static void setJsonRawMessageState(JsonObject rawJsonMessage, JsonObject headers, Object state) {
        if (state != null) {
            if (headers == null)
                rawJsonMessage.set(JsonBusConstants.HEADERS, headers = Json.createObject());
            headers.set(JsonBusConstants.HEADERS_STATE, StateAccessor.encodeState(state));
        }
    }

    private static Consumer<Object> clientLiveListener;

    public static void setClientLiveListener(Consumer<Object> clientLiveListener) {
        ServerJsonBusStateManager.clientLiveListener = clientLiveListener;
    }

    public static void clientIsLive(Object state, Session session) {
        if (clientLiveListener != null) {
            String runId = StateAccessor.getRunId(state);
            if (runId == null) {
                runId = SessionAccessor.getRunId(session);
                if (runId == null) {
                    session = session.get(ASSOCIATED_SESSION_KEY);
                    if (session != null)
                        runId = SessionAccessor.getRunId(session);
                }
            }
            if (runId != null)
                clientLiveListener.accept(runId);
        }
    }

}
