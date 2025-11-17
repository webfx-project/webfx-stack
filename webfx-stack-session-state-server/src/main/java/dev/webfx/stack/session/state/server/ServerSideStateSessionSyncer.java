package dev.webfx.stack.session.state.server;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.tuples.Pair;
import dev.webfx.stack.authn.logout.server.LogoutPush;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.state.LogoutUserId;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class ServerSideStateSessionSyncer {

    private final static boolean LOG_STATES = false; // Set to true to log incoming and outgoing states on the server side

    private static AsyncFunction<Object, Object> userIdChecker;

    public static void setUserIdChecker(AsyncFunction<Object, Object> userIdChecker) {
        ServerSideStateSessionSyncer.userIdChecker = userIdChecker;
    }

    private static AsyncFunction<Void, Void> userIdAuthorizer;

    public static void setUserIdAuthorizer(AsyncFunction<Void, Void> userIdAuthorizer) {
        ServerSideStateSessionSyncer.userIdAuthorizer = userIdAuthorizer;
    }

    // ======================================== INCOMING STATE ON SERVER ========================================
    // Sync method to be used on the server side, when the server receives an incoming state from a client

    public static Future<Pair<Session /* final server session */, Object/* final incoming state*/>> syncIncomingState(Session serverSession, Object incomingState) {
        String incomingStateCapture = LOG_STATES ? "" + incomingState : null; // capturing state before changes for logs

        Future<Session> sessionFuture;

        // serverSession.id <= incomingState.serverSessionId ? ONLY ON NEW SERVER SESSION
        String requestedServerSessionId = StateAccessor.getServerSessionId(incomingState);
        String serverSessionRunId = SessionAccessor.getRunId(serverSession);
        boolean isNewServerSession = serverSessionRunId == null;
        if (requestedServerSessionId != null /*&& isNewServerSession*/ && !Objects.equals(requestedServerSessionId, serverSession.id())) {
            sessionFuture = SessionService.getSessionStore().get(requestedServerSessionId)
                .compose(loadedSession ->
                    syncFixedServerSessionFromIncomingClientStateWithUserIdCheckFirst(loadedSession != null ? loadedSession : serverSession, incomingState, false));
        } else
            sessionFuture = syncFixedServerSessionFromIncomingClientStateWithUserIdCheckFirst(serverSession, incomingState, isNewServerSession);

        return sessionFuture.map(finalServerSession -> {
            // Finally, we enrich the incoming state with possible further info coming from the serverSession
            Object finalIncomingState = ServerSideStateSessionSyncer.syncIncomingClientStateFromServerSession(incomingState, finalServerSession);

            if (LOG_STATES)
                Console.log("👉👉 Incoming state: " + incomingStateCapture + " >> " + finalIncomingState);

            return new Pair<>(finalServerSession, finalIncomingState);
        });
    }

    private static Future<Session> syncFixedServerSessionFromIncomingClientStateWithUserIdCheckFirst(Session serverSession, Object clientState, boolean forceStore) {
        Object userId = StateAccessor.getUserId(clientState);
        // Case when the user hasn't changed (userId == null => not yet logged in or is the same user as last time in this server session)
        if (userId == null || userIdChecker == null)
            return syncFixedServerSessionFromIncomingClientState(serverSession, clientState, forceStore);
        // Case when the user is set => login or user switch, or logout (LOGOUT_USER_ID)
        return ThreadLocalStateHolder.runWithState(clientState, () -> userIdChecker.apply(userId))
            .compose(finalUserId -> {
                // Setting the new user id (should be the same as the passed on if valid, or something like "INVALID" if not)
                Console.log("️🛡 UserIdCheck: userId=" + userId + " => finalUserId = " + finalUserId);
                // If the user identity check failed, we log out the user
                if (finalUserId == null)
                    finalUserId = LogoutUserId.LOGOUT_USER_ID;
                // Memorizing the final user id in the client state
                StateAccessor.setUserId(clientState, finalUserId);
                // We continue with the normal session <-> state sync process
                Future<Session> future = syncFixedServerSessionFromIncomingClientState(serverSession, clientState, forceStore);
                // At the same time, we do a push to the client of either the logout userId (if it's a logout), or the
                // new authorizations (if it's a login or user switch). To prepare this push, we need to ensure that the
                // userId is set in the client state (the runId is what identifies which client to push to).
                if (StateAccessor.getRunId(clientState) == null) // // if not, we set it from the server session
                    StateAccessor.setRunId(clientState, SessionAccessor.getRunId(serverSession));
                // We are now ready for the push
                ThreadLocalStateHolder.runWithState(clientState, () -> { // we specify which state to use for the push
                    // Special case: invalid user => we force a logout
                    if (LogoutUserId.isLogoutUserId(ThreadLocalStateHolder.getUserId())) {
                        LogoutPush.pushLogoutMessageToClient(); // This will push a logout userId, and subsequently push the new authorizations (see OUTGOING STATE)
                        // General case: valid user (probably a user switch from the client, or a reconnection)
                    } else if (userIdAuthorizer != null) {
                        // We ask the authorizer to push the new authorizations for that user
                        // Note: that push shouldn't contain the userId, otherwise this will create a loop (see OUTGOING STATE).
                        userIdAuthorizer.apply(null);
                    }
                });
                return future;
            });
    }

    private static Future<Session> syncFixedServerSessionFromIncomingClientState(Session serverSession, Object clientState, boolean forceStore) {
        // serverSession.userId <= clientState.userId ? YES IF SET, as this means the client switched user, so we memorise that info in the session
        boolean userIdChanged = SessionAccessor.changeUserId(serverSession, StateAccessor.getUserId(clientState), true);
        // serverSession.runId <= clientState.runId ? YES IF SET, as this means the client is communicating the run id, so we memorise that in the session
        String runId = StateAccessor.getRunId(clientState);
        boolean runIdChanged = SessionAccessor.changeRunId(serverSession, runId, true);
        // serverSession.backoffice <= clientState.backoffice ? YES IF SET, as this means the client is communicating the client type, so we memorise that in the session
        Boolean backoffice = StateAccessor.getBackoffice(clientState);
        boolean backofficeChanged = SessionAccessor.changeBackoffice(serverSession, backoffice, true);
        // Since clients communicate the runId on first connection or reconnection, the sessionId must be synced in both cases (on reconnection, the session id may have changed)
        boolean sessionIdSyncedChanged = runId != null && SessionAccessor.changeServerSessionIdSynced(serverSession, false);
        if (userIdChanged || runIdChanged || backofficeChanged || sessionIdSyncedChanged || forceStore)
            return storeServerSession(serverSession);
        return Future.succeededFuture(serverSession);
    }

    private static Object syncIncomingClientStateFromServerSession(Object clientState, Session serverSession) {
        // clientState.serverSessionId <= serverSession.id ? ALWAYS, because this is the server session that is responsible for the session id
        clientState = StateAccessor.setServerSessionId(clientState, serverSession.id(), true);
        // clientState.userId <= serverSession.userId ? YES IF NOT SET, otherwise this means the client switched user, so we keep that info
        clientState = StateAccessor.setUserId(clientState, SessionAccessor.getUserId(serverSession), false);
        // clientState.runId <= serverSession.runId ? YES IF NOT SET, otherwise this means the client communicates it, so we keep that info
        clientState = StateAccessor.setRunId(clientState, SessionAccessor.getRunId(serverSession), false);
        // clientState.backoffice <= serverSession.backoffice ? YES IF NOT SET, otherwise this means the client communicates it, so we keep that info
        clientState = StateAccessor.setBackoffice(clientState, SessionAccessor.isBackoffice(serverSession), false);
        return clientState;
    }

    private static Future<Session> storeServerSession(Session serverSession) {
        return serverSession.store()
            .onFailure(Console::log)
            .map(x -> serverSession);
    }

    // ======================================== OUTGOING STATE ON SERVER ========================================
    // Sync methods to be used on the server side, when the server is about to send a state generated by the server back to the client

    public static Object syncOutgoingState(Object outgoingState, Session serverSession) {
        String outgoingStateCapture = LOG_STATES ? "" + outgoingState : null; // capturing state before changes for logs

        // serverSession.id <= outgoingState.serverSessionId ? NEVER (serverSession.id can't be changed at this point)
        // serverSession.userId <= outgoingState.userId ? YES IF SET, as this means the server switched or logged-in user, so we memorize that info in the session
        boolean userIdChanged = SessionAccessor.changeUserId(serverSession, StateAccessor.getUserId(outgoingState), true);
        // serverSession.runId <= outgoingState.runId ? NEVER, because this is info can only come from an incoming outgoingState.
        // outgoingState.sessionId <= serverSession.id ? ONLY if the client doesn't know it already
        boolean sessionIdSyncedChanged = false;
        if (!SessionAccessor.isServerSessionIdSynced(serverSession)) {
            outgoingState = StateAccessor.setServerSessionId(outgoingState, serverSession.id(), true);
            sessionIdSyncedChanged = SessionAccessor.changeServerSessionIdSynced(serverSession, true);
        }
        // outgoingState.userId <= serverSession.userId ? NO, we communicate this info only once to the client (when the server code explicitly sets outgoingState.userId)
        // outgoingState.runId <= serverSession.runId ? NEVER (ERASED), because it's always communicated in the opposite way (client => server)
        // outgoingState.backoffice <= serverSession.backoffice ? NEVER (ERASED), because it's always communicated in the opposite way (client => server)
        outgoingState = StateAccessor.setRunId(outgoingState, null, true);
        if (userIdChanged || sessionIdSyncedChanged)
            storeServerSession(serverSession);

        // Authorization push management:
        // If a user id is set in that direction, this means the server switched, logged-in or logged-out the user,
        // so we need in all cases to call the authorizer to push the new authorizations to the client.
        // Note: that authorizations push shouldn't contain the user id to avoid a loop here.
        if (userIdChanged && userIdAuthorizer != null) {
            // It's important to set the userId and runId in ThreadLocalStateHolder before calling userIdAuthorizer
            // because it will load the authorizations from userId and push them to the runId client.

            // Most of the time the runId is in the server session, which matches the associated client. An exception to
            // that rule is the magic link, where the magic link client and the login client are different. If the magic
            // link is valid, the authorizations must be pushed to the login client and not to the magic link client
            // associated with this session. The magic link AuthenticationGatewayProvider indicated this by setting the
            // login client runId in the server state.

            // Creating a new state from the session => should contain the userId, runId, and eventually other info (ex: backoffice)
            Object state = StateAccessor.createStateFromSession(serverSession);
            ThreadLocalStateHolder.runWithState(state, () -> userIdAuthorizer.apply(null));
        }

        if (LOG_STATES)
            Console.log("👈👈 Outgoing state: " + outgoingState + " << " + outgoingStateCapture);

        return outgoingState;
    }

}
