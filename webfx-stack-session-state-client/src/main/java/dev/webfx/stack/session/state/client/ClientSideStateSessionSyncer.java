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

    // Logged at most once per client run if a server path forgets to stamp serverOrigin, to avoid
    // turning every incoming message into a warning when a regression slips through.
    private static boolean missingServerOriginWarned;

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

        // Apply server-side updates only when the envelope is stamped serverOrigin=true. Anything
        // else is typically a peer-to-peer broadcast leaking a publisher's headers — the bus also
        // strips those before forwarding (defense in depth). We still enrich the state with the
        // local client session below so downstream handlers see this client's identity.
        boolean serverOriginated = StateAccessor.isServerOrigin(incomingState);
        if (!serverOriginated && incomingState != null && !missingServerOriginWarned) {
            missingServerOriginWarned = true;
            Console.warn("Ignoring incoming state without serverOrigin marker (warned once per run): " + incomingState);
        }

        // ================== 1) We update the client session from the incoming state if necessary =====================

        if (serverOriginated) {
            clientSideStateSession.incrementServerIncomingMessageSequence();
            // clientSession.sessionId <= incomingState.sessionId ? YES IF SET, because this means the server communicated the session id
            clientSideStateSession.changeServerSessionId(StateAccessor.getServerSessionId(incomingState), true, true);
            // clientSession.userToken <= incomingState.userToken ? YES IF SET, as this means the server minted a new one.
            // BEFORE the user id, and the order is the protection rather than a detail. A logout arrives as
            // LOGOUT_USER_ID and changeUserId clears the token with it; applied in the other order, a token riding
            // along in that same message would be written back AFTER the logout had cleared it, handing the client a
            // working proof of the identity it had just been told to forget. The comment here used to claim that
            // protection while the code did the reverse — and it went unnoticed because nothing ever sent a token
            // and a logout together, until renewal started minting tokens outside the login path.
            clientSideStateSession.changeUserToken(StateAccessor.getUserToken(incomingState), true);
            // clientSession.userId <= incomingState.userId ? YES IF SET, as this means the server communicates the user id
            clientSideStateSession.changeUserId(StateAccessor.getUserId(incomingState), true, true);
            // Whether this server still accepts a bare claim. Server to client only — believing it makes us
            // send less, never more, so a wrong value cannot manufacture an identity.
            clientSideStateSession.setTokenRequired(StateAccessor.isTokenRequired(incomingState));
            // clientSession.runId <= incomingState.runId ? NEVER, as the server never communicates it (and is not supposed to)
            // The runId is not stored in the client session anyway (as it's a different id on each run)
        }

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
        // outgoingState.userToken <= clientSession.userToken ? ALWAYS (the server checks it on every message, so
        // "if not yet sent" would leave almost every message unprovable — see setOutgoingUserToken)
        outgoingState = clientSideStateSession.setOutgoingUserToken(outgoingState);
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
