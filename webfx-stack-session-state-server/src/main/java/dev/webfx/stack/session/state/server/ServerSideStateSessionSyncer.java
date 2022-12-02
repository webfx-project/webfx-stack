package dev.webfx.stack.session.state.server;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.console.Console;
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

    private static AsyncFunction<Object, Object> userIdChecker;

    public static void setUserIdChecker(AsyncFunction<Object, Object> userIdChecker) {
        ServerSideStateSessionSyncer.userIdChecker = userIdChecker;
    }

    private static AsyncFunction<Void, Void> userIdAuthorizer;

    public static void setUserIdAuthorizer(AsyncFunction<Void, Void> userIdAuthorizer) {
        ServerSideStateSessionSyncer.userIdAuthorizer = userIdAuthorizer;
    }

    // ======================================== INCOMING STATE ON SERVER ========================================
    // Sync methods to be used on server side, when the server receives an incoming state from a client

    public static Future<Session> syncServerSessionFromIncomingClientState(Session serverSession, Object clientState) {
        // serverSession.id <= clientState.serverSessionId ? ONLY ON NEW SERVER SESSION
        String requestedServerSessionId = StateAccessor.getServerSessionId(clientState);
        boolean newServerSession = SessionAccessor.getRunId(serverSession) == null;
        if (requestedServerSessionId != null && newServerSession && !Objects.equals(requestedServerSessionId, serverSession.id())) {
            Promise<Session> promise = Promise.promise();
            SessionService.getSessionStore().get(requestedServerSessionId).onComplete(ar -> {
                Session requestedSession = ar.result();
                syncFixedServerSessionFromIncomingClientStateWithUserIdCheckFirst(requestedSession != null ? requestedSession : serverSession, clientState, false)
                        .onFailure(promise::fail)
                        .onSuccess(promise::complete);
            });
            return promise.future();
        }
        return syncFixedServerSessionFromIncomingClientStateWithUserIdCheckFirst(serverSession, clientState, newServerSession);
    }

    private static Future<Session> syncFixedServerSessionFromIncomingClientStateWithUserIdCheckFirst(Session serverSession, Object clientState, boolean forceStore) {
        Object userId = StateAccessor.getUserId(clientState);
        if (userId == null || userIdChecker == null)
            return syncFixedServerSessionFromIncomingClientState(serverSession, clientState, forceStore);
        Promise<Session> promise = Promise.promise();
        ThreadLocalStateHolder.runWithState(clientState, () -> userIdChecker.apply(userId))
                .onComplete(ar -> {
                    // Setting the new user id (should be the same as the passed on if valid, or something like "INVALID" if not)
                    Object finalUserId = ar.result();
                    // If the user identity check failed, we log out the user
                    if (finalUserId == null)
                        finalUserId = LogoutUserId.LOGOUT_USER_ID;
                    // Memorizing the final user id in the client state
                    StateAccessor.setUserId(clientState, finalUserId);
                    // We continue with the normal session <-> state sync process
                    syncFixedServerSessionFromIncomingClientState(serverSession, clientState, forceStore)
                            .onFailure(promise::fail)
                            .onSuccess(promise::complete);
                    // We set the runId from the session if not set in the state
                    if (StateAccessor.getRunId(clientState) == null)
                        StateAccessor.setRunId(clientState, SessionAccessor.getRunId(serverSession));
                    // We log out the client or push the authorizations of the user
                    ThreadLocalStateHolder.runWithState(clientState, () -> {
                        // Special case: invalid user => we force a logout
                        if (LogoutUserId.isLogoutUserId(ThreadLocalStateHolder.getUserId()))
                            LogoutPush.pushLogoutMessageToClient(); // This will push a logout userId, and subsequently push the new authorizations (see OUTGOING STATE)
                        // General case: valid user (probably a user switch from the client, or a reconnection)
                        else if (userIdAuthorizer != null)
                            // We ask the authorizer to push the new authorizations for that user
                            // Note: that push shouldn't contain the userId, otherwise this will create a loop (see OUTGOING STATE).
                            userIdAuthorizer.apply(null);
                    });
                });
        return promise.future();
    }

    private static Future<Session> syncFixedServerSessionFromIncomingClientState(Session serverSession, Object clientState, boolean forceStore) {
        // serverSession.userId <= clientState.userId ? YES IF SET, as this means the client switched user, so we memorise that info in the session
        boolean userIdChanged = SessionAccessor.changeUserId(serverSession, StateAccessor.getUserId(clientState), true);
        // serverSession.runId <= clientState.runId ? YES IF SET, as this means the client is communicating the run id, so we memorise that in the session
        String runId = StateAccessor.getRunId(clientState);
        boolean runIdChanged = SessionAccessor.changeRunId(serverSession, runId, true);
        // Since clients communicate the runId on first connection or reconnection, the sessionId must be synced in both cases (on reconnection, the session id may have changed)
        boolean sessionIdSyncedChanged = runId != null && SessionAccessor.changeServerSessionIdSynced(serverSession, false);
        if (userIdChanged || runIdChanged || sessionIdSyncedChanged || forceStore)
            return storeServerSession(serverSession);
        return Future.succeededFuture(serverSession);
    }

    private static Future<Session> storeServerSession(Session serverSession) {
        Future<Boolean> future = SessionService.getSessionStore().put(serverSession);
        future.onFailure(Console::log);
        return future.map(x -> serverSession);
    }

    public static Object syncIncomingClientStateFromServerSession(Object clientState, Session serverSession) {
        // clientState.serverSessionId <= serverSession.id ? ALWAYS, because this is the server session that is responsible for the session id
        clientState = StateAccessor.setServerSessionId(clientState, serverSession.id(), true);
        // clientState.userId <= serverSession.userId ? YES IF NOT SET, otherwise this means the client switched user, so we keep that info
        clientState = StateAccessor.setUserId(clientState, SessionAccessor.getUserId(serverSession), false);
        // clientState.runId <= serverSession.runId ? YES IF NOT SET, otherwise this means the client communicates it, so we keep that info
        clientState = StateAccessor.setRunId(clientState, SessionAccessor.getRunId(serverSession), false);
        return clientState;
    }


    // ======================================== OUTGOING STATE ON SERVER ========================================
    // Sync methods to be used on server side, when the server is about to send a state generated by the server back to the client

    public static Object syncOutgoingServerStateFromServerSessionAndViceVersa(Object serverState, Session serverSession) {
        // If a user id is set in that direction, this means the server switched, logged-in or logged-out the user,
        // so we need in all cases to call the authorizer to push the new authorizations to the client.
        // Note: that authorizations push shouldn't contain the user id to avoid a loop here.
        Object userId = StateAccessor.getUserId(serverState);
        if (userId != null && userIdAuthorizer != null)
            ThreadLocalStateHolder.runWithState(StateAccessor.setUserId(StateAccessor.setRunId(null, SessionAccessor.getRunId(serverSession)), userId), () -> userIdAuthorizer.apply(null));
        // serverSession.id <= serverState.serverSessionId ? NEVER (serverSession.id can't be changed at this point)
        // serverSession.userId <= serverState.userId ? YES IF SET, as this means the server switched or logged-in user, so we memorise that info in the session
        boolean userIdChanged = SessionAccessor.changeUserId(serverSession, userId, true);
        // serverSession.runId <= serverState.runId ? NEVER, because this is info can only come from an incoming serverState.
        // serverState.sessionId <= serverSession.id ? ONLY if the client doesn't know it already
        boolean sessionIdSyncedChanged = false;
        if (!SessionAccessor.isServerSessionIdSynced(serverSession)) {
            serverState = StateAccessor.setServerSessionId(serverState, serverSession.id(), true);
            sessionIdSyncedChanged = SessionAccessor.changeServerSessionIdSynced(serverSession, true);
        }
        // serverState.userId <= serverSession.userId ? NO, we communicate this info only once to the client (when the server code explicitly sets serverState.userId)
        // serverState.runId <= serverSession.runId ? NEVER (ERASED), because it's always communicated in the opposite way (client => server)
        serverState = StateAccessor.setRunId(serverState, null, true);
        if (userIdChanged || sessionIdSyncedChanged)
            storeServerSession(serverSession);
        return serverState;
    }

}
