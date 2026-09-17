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
import dev.webfx.stack.session.isolation.IsolatedSession;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.server.ServerSideStateSessionSyncer;



/**
 * @author Bruno Salmon
 */
public final class ServerJsonBusStateManager implements JsonBusConstants {

    private final static boolean LOG_RAW_MESSAGES = false;

    public static void initialiseStateManagement(Bus serverJsonBus) {
        // We register at PING_STATE_ADDRESS a handler that just replies to the client who sent that ping state with an
        // empty body. What's important here is not the body, but the triggering of the state mechanism that will
        // consider
        serverJsonBus.register(JsonBusConstants.PING_STATE_ADDRESS, message -> message.reply(null, new DeliveryOptions()));
    }

    public static Future<IsolatedSession> manageStateOnIncomingOrOutgoingRawJsonMessage(AstObject rawJsonMessage, IsolatedSession serverSession, boolean incoming) {
        AstObject headers = rawJsonMessage.getObject(JsonBusConstants.HEADERS);
        Object originalState = headers == null ? null : StateAccessor.decodeState(headers.getString(JsonBusConstants.HEADERS_STATE));

        // Incoming message (from client to server)
        if (incoming) {
            if (LOG_RAW_MESSAGES)
                Console.log(">> Incoming message : " + Json.formatNode(rawJsonMessage));
            // Mark it as having come from outside. The mirror of the serverOrigin stamp below, and
            // deliberately NOT its complement: both are positive assertions, and neither should ever be
            // derived from the absence of the other — see the note on setOutgoingJsonRawMessageState.
            // This is the ONLY place a client message can enter, so
            // it is the only place that can say so with authority — and it overwrites rather than
            // defaults, so a caller who sends clientOrigin:false is corrected rather than believed. The
            // asymmetry is the whole value: server-internal work never passes through the bridge and so
            // never carries the stamp, which is what lets a rule distinguish "a client asked for this"
            // from "the server did it", something no endpoint downstream can tell for itself.
            //
            // Stamped BEFORE the session sync so everything past this point, including the sync's own
            // decisions, sees it. A message that arrived with no state at all gets one created here: the
            // absence of a state header must not be a way to arrive unstamped.
            originalState = StateAccessor.setClientOrigin(originalState, true);
            // We sync the application serverSession with the incoming state. This is at this point that the serverSession
            // switch can happen if requested by the client, in which case a different serverSession will be returned.
            return ServerSideStateSessionSyncer.syncIncomingState(serverSession, originalState)
                    .compose(pair -> {
                        // Getting the final session and incoming state as a result
                        IsolatedSession finalServerSession = pair.get1();
                        Object finalIncomingState = pair.get2();
                        // We memorize that final state in the raw message
                        setJsonRawMessageState(rawJsonMessage, headers, finalIncomingState);
                        // We tell the client is live
                        clientIsLive(finalIncomingState, finalServerSession, false);
                        // We tell the message delivery can now continue into the server and return the serverSession (not
                        // sure if the serverSession object will be useful - the most important thing is to complete this
                        // asynchronous operation so the delivery can go on)
                        return Future.succeededFuture(finalServerSession);
                    });
        }

        // Outgoing message (from server to client)
        // We enrich the state with possible further info coming from the serverSession (ex: serverSessionId change)
        Object finalOutgoingState = ServerSideStateSessionSyncer.syncOutgoingState(originalState, serverSession);
        // We pass that final state inside the raw message
        setOutgoingJsonRawMessageState(rawJsonMessage, headers, finalOutgoingState);
        if (LOG_RAW_MESSAGES)
            Console.log("<< Outgoing message : " + Json.formatNode(rawJsonMessage));
        // We tell the message delivery can now continue into the client and return the serverSession (not sure if the serverSession
        // object is useful - the most important thing is to complete this asynchronous operation so the delivery can go on)
        return Future.succeededFuture(serverSession);
    }

    public static void setJsonRawMessageState(AstObject rawJsonMessage, AstObject headers, Object state) {
        if (state != null) {
            if (headers == null)
                rawJsonMessage.set(JsonBusConstants.HEADERS, headers = AST.createObject());
            headers.set(JsonBusConstants.HEADERS_STATE, StateAccessor.encodeState(state));
        }
    }

    // Use this when writing a state that is leaving the server toward a client. It stamps the
    // serverOrigin marker so the recipient distinguishes authoritative server pushes from
    // peer-to-peer broadcast headers leaked by a publisher.
    //
    // NOT the complement of clientOrigin, and deliberately not derived from it. Both are POSITIVE
    // assertions, which is what makes each safe: a reader trusts only what is affirmed, so a message
    // carrying neither marker is refused by both rules. Rewriting either as the other's negation would
    // turn "trust what is affirmed" into "trust what is not denied", and absence is the default state
    // of the world — the failure mode would move from someone having to lie to someone having to
    // forget, and forgetting is far commoner. They are also stamped by different components that know
    // different things, and read by parties with different powers to verify. The symmetry is real; the
    // equivalence is not.
    public static void setOutgoingJsonRawMessageState(AstObject rawJsonMessage, AstObject headers, Object state) {
        if (state != null)
            StateAccessor.setServerOrigin(state, true);
        setJsonRawMessageState(rawJsonMessage, headers, state);
    }

    /**
     * Notified when a client is confirmed live, carrying its runId plus the session facts the push
     * layer records for the /monitor page (current userId, build version, PWA mode, device profile,
     * BO/FO app) — without this module depending on it. {@code userId} reflects the session's CURRENT
     * login (re-read each tick).
     */
    @FunctionalInterface
    public interface ClientLiveListener {
        void onClientLive(Object runId, Object userId, String clientVersion, Boolean pwa, String clientProfile, Boolean backoffice);
    }

    private static ClientLiveListener clientLiveListener;

    public static void setClientLiveListener(ClientLiveListener clientLiveListener) {
        ServerJsonBusStateManager.clientLiveListener = clientLiveListener;
    }

    public static boolean clientIsLive(Object state, Session session, boolean ping) {
        if (clientLiveListener != null) {
            // Trying to get the client runId from the state
            String runId = StateAccessor.getRunId(state);
            if (runId == null) {
                // If not found, trying to get it from the session
                runId = SessionAccessor.getRunId(session);
            }
            if (runId != null) {
                // Read the invariant client facts from the session (the client sent them once at
                // connection). Re-supplied on every live tick so a push entry created after connect
                // still picks them up.
                clientLiveListener.onClientLive(runId, SessionAccessor.getUserId(session), SessionAccessor.getClientVersion(session), SessionAccessor.getPwa(session), SessionAccessor.getClientProfile(session), SessionAccessor.isBackoffice(session));
                return true; // to tell that we found the runId
            }
            Console.warn("ServerJsonBusStateManager.clientIsLive() was called but no runId could be found (session id = " + session.id() + ", ping = " + ping + ")");
        }
        return false;
    }

}
