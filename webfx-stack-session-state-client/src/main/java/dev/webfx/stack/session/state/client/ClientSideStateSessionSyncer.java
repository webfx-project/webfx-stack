package dev.webfx.stack.session.state.client;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;

/**
 * @author Bruno Salmon
 */
public final class ClientSideStateSessionSyncer {

    private static final boolean LOG_STATES = false; // Set to true to log incoming and outgoing states on the client side

    private static ClientSideStateSession getClientSideStateSession() {
        return ClientSideStateSession.getInstance();
    }

    // ======================================== CONNECTION STATE MANAGEMENT ===========================================

    public static void setClientConnected(boolean connected) {
        setClientConnected(getClientSideStateSession(), connected);
    }

    public static void setClientConnected(ClientSideStateSession clientSideStateSession, boolean connected) {
        clientSideStateSession.changeConnected(connected);
    }


    // ======================================== INCOMING STATE ON CLIENT ========================================
    // Sync method to be used on the client side, when the client receives an incoming state from the server

    public static Object syncIncomingState(Object incomingState) {
        Object incomingStateCapture = LOG_STATES ? "" + incomingState : null;

        ClientSideStateSession clientSideStateSession = getClientSideStateSession();
        clientSideStateSession.incrementServerIncomingMessageSequence();

        // ================== 1) We update the client session from the incoming state if necessary =====================

        // clientSession.sessionId <= incomingState.sessionId ? YES IF SET, because this means the server communicated the session id
        clientSideStateSession.changeServerSessionId(StateAccessor.getServerSessionId(incomingState), true, true);
        // clientSession.userId <= incomingState.userId ? YES IF SET, as this means the server communicates the user id
        clientSideStateSession.changeUserId(StateAccessor.getUserId(incomingState), true, true);
        // clientSession.runId <= incomingState.runId ? NEVER, as the server never communicates it (and is not supposed to)
        // The runId is not stored in the client session anyway (as it's a different id on each run)

        // ============ 2) We eventually enrich the incoming state with information from the client session ============

        Session clientSession = clientSideStateSession.getClientSession();
        // incomingState.serverSessionId <= clientSession.serverSessionId ? YES IF NOT SET (ie we keep the session value if the server didn't refresh the sessionId)
        incomingState = StateAccessor.setServerSessionId(incomingState, SessionAccessor.getServerSessionId(clientSession), false);
        // incomingState.userId <= clientSession.userId ? YES IF NOT SET (ie we keep the session value if the server didn't refresh the userId)
        incomingState = StateAccessor.setUserId(incomingState, SessionAccessor.getUserId(clientSession), false);
        // incomingState.runId <= runId ? ALWAYS (but we actually take it from the memory - not the session)
        incomingState = StateAccessor.setRunId(incomingState, clientSideStateSession.getRunId(), true);
        // incomingState.backoffice <= backoffice ? ALWAYS (but we actually take it from the memory - not the session)
        incomingState = StateAccessor.setBackoffice(incomingState, clientSideStateSession.isBackoffice(), true);

        if (LOG_STATES)
            Console.log("👈👈 Incoming sate: " + incomingState + " << " + incomingStateCapture);

        // We return the enriched incoming state
        return incomingState;
    }

    // ======================================== OUTGOING STATE ON CLIENT ========================================
    // Sync method to be used on the client side, when the client is about to send an outgoing state to the server

    public static Object syncOutgoingState(Object outgoingState) {
        Object outgoingStateCapture = LOG_STATES ? "" + outgoingState : null;

        ClientSideStateSession clientSideStateSession = getClientSideStateSession();

        // ============ 1) We eventually enrich the outgoing state with information stored from the client =============

        // outgoingState.sessionId <= clientSession.id ? YES IF NOT YET SENT TO SERVER
        outgoingState = clientSideStateSession.setOutgoingServerSessionIdIfNotYetSent(outgoingState);
        // outgoingState.userId <= clientSession.userId ? YES IF NOT YET SENT TO SERVER
        outgoingState = clientSideStateSession.setOutgoingUserIdIfNotYetSent(outgoingState);
        // outgoingState.runId <= clientSession.runId ? YES IF NOT YET SENT TO SERVER
        outgoingState = clientSideStateSession.setOutgoingRunIdIfNotYetSent(outgoingState);
        // outgoingState.backoffice <= clientSession.backoffice ? YES IF NOT YET SENT TO SERVER
        outgoingState = clientSideStateSession.setOutgoingBackofficeIfNotYetSent(outgoingState);

        // 2) We update the client session from the outgoing state if necessary

        // clientSession.sessionId <= outgoingState.sessionId ? YES IF SET
        clientSideStateSession.changeServerSessionId(StateAccessor.getServerSessionId(outgoingState), true, false);
        // clientSession.userId <= outgoingState.userId ? YES IF SET
        clientSideStateSession.changeUserId(StateAccessor.getUserId(outgoingState), true, false);
        // clientSession.runId <= outgoingState.runId ? YES IF SET
        clientSideStateSession.changeRunId(StateAccessor.getRunId(outgoingState), true);
        // clientSession.backoffice <= outgoingState.backoffice ? NEVER (no need to store it in the session as it's inherent to the client)

        if (LOG_STATES)
            Console.log("👉👉 Outgoing sate: " + outgoingStateCapture + " >> " + outgoingState);

        // We return the enriched outgoing state
        return outgoingState;
    }

}
