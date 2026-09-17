package dev.webfx.stack.session.state.client;

import dev.webfx.kit.launcher.WebFxKitLauncher;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.meta.Meta;
import dev.webfx.platform.storage.LocalStorage;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.useragent.UserAgent;
import dev.webfx.platform.util.uuid.Uuid;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.SessionStore;
import dev.webfx.stack.session.state.LogoutUserId;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;

/**
 * @author Bruno Salmon
 */
public final class ClientSideStateSession {

    // We prefix the key used to store the active session id in the browser, because it is stored in the local storage
    // which is shared with all other apps running under the same origin. This will prevent interferences between apps
    // (especially webfx apps). Not necessary for other platforms as the client session storage is already specific to
    // the application.
    private static final String ACTIVE_CLIENT_SESSION_ID_PREFIX = UserAgent.isBrowser() ? Meta.getApplicationModuleName() + "-" : "";
    private static final String ACTIVE_CLIENT_SESSION_ID = ACTIVE_CLIENT_SESSION_ID_PREFIX + "activeClientSessionId";
    private static final String RUN_ID = Uuid.randomUuid();
    private static final Boolean BACKOFFICE = Meta.getBackoffice(); // Backoffice applications should set this flag to true
    private static final ClientSideStateSession INSTANCE = new ClientSideStateSession();

    public static ClientSideStateSession getInstance() {
        return INSTANCE;
    }

    private final SessionStore sessionStore;
    private Session clientSession;
    private boolean clientSessionChanged;
    private boolean serverSessionIdChanged;
    private boolean userIdChanged;
    private boolean runIdChanged;
    private boolean connected;
    private boolean connectedChanged;
    private Runnable scheduledSessionStore;
    private Runnable scheduledListenerCall;
    private int serverIncomingMessageSequence;
    /**
     * The server's statement that it no longer accepts a bare claim. In memory only, never stored: it is
     * re-learned on the first server message of each connection, and the fallback while unknown — send the
     * user id as well — is the safe one. A stored `true` could outlive a rollback of the flip and leave a
     * client silently declining to say who it is to a server that still needs to be told.
     */
    private boolean tokenRequired;

    private ClientSideStateSessionListener clientSideStateSessionListener;

    public ClientSideStateSession() {
        this(SessionService.getSessionStore());
    }

    public ClientSideStateSession(SessionStore sessionStore) {
        this(sessionStore, sessionStore.createSession(Long.MAX_VALUE));
        String clientSessionId = LocalStorage.getItem(ACTIVE_CLIENT_SESSION_ID);
        if (clientSessionId != null)
            sessionStore.get(clientSessionId)
                .onComplete(ar -> {
                    Session session = ar.result();
                    if (session != null)
                        setClientSession(session);
                    else {
                        Console.log("WARNING: Couldn't reload previous client session from session store (session with localId " + clientSessionId + " not found)");
                        if (ar.failed())
                            Console.error(ar.cause());
                    }
                });
    }

    public ClientSideStateSession(SessionStore sessionStore, Session clientSession) {
        this.sessionStore = sessionStore;
        this.clientSession = clientSession;
    }

    public void incrementServerIncomingMessageSequence() {
        serverIncomingMessageSequence++;
    }

    private void scheduleSessionStoreAndListenerCall() {
        scheduleSessionStorage();
        scheduleListenerCall();
    }

    private void scheduleSessionStorage() {
        if (scheduledSessionStore == null)
            UiScheduler.scheduleDeferred(scheduledSessionStore = () -> {
                sessionStore.put(clientSession);
                LocalStorage.setItem(ACTIVE_CLIENT_SESSION_ID, clientSession.id());
                scheduledSessionStore = null;
            });
    }

    private void scheduleListenerCall() {
        if (scheduledListenerCall == null)
            callRunnable(scheduledListenerCall = () -> {
                callListener();
                scheduledListenerCall = null;
            });
    }

    private void callListener() {
        callRunnable(() -> {
            ClientSideStateSessionListener listener = clientSideStateSessionListener;
            if (listener != null) {
                if (clientSessionChanged) {
                    clientSessionChanged = false;
                    listener.onClientSessionChanged(getClientSession());
                    // Because we switched the session (or loaded it on start), we need to update all other settings
                    serverSessionIdChanged = userIdChanged = runIdChanged = connectedChanged = true;
                }
                if (serverSessionIdChanged) {
                    serverSessionIdChanged = false;
                    listener.onServerSessionIdChanged(getServerSessionId());
                }
                if (userIdChanged) {
                    userIdChanged = false;
                    listener.onUserIdChanged(getUserId());
                }
                if (runIdChanged) {
                    runIdChanged = false;
                    listener.onRunIdChanged(getRunId());
                }
                if (connectedChanged) {
                    connectedChanged = false;
                    listener.onConnectedChanged(isConnected());
                }
            }
        });
    }

    private void callRunnable(Runnable runnable) {
        // When the JavaFX UI has not yet started (only the application logic started), we don't postpone the call in
        // the UI thread, we run it immediately, because:
        // 1) there is no danger of UI thread exception at this point
        // 2) the sequencing can be very sensitive on boot time, and postponing the call will probably alter the boot
        // sequence and create problems.
        if (!WebFxKitLauncher.isReady())
            runnable.run();
        else // Once the JavaFX UI has started, we ensure it runs in the UI thread
            UiScheduler.runInUiThread(runnable);
    }

    public void setClientSideStateSessionHolder(ClientSideStateSessionListener clientSideStateSessionListener) {
        this.clientSideStateSessionListener = clientSideStateSessionListener;
        callListener();
    }

    public Session getClientSession() {
        return clientSession;
    }

    public void setClientSession(Session clientSession) {
        if (clientSession != this.clientSession) {
            this.clientSession = clientSession;
            clientSessionChanged = true;
            scheduleSessionStoreAndListenerCall();
        }
    }

    public String getServerSessionId() {
        return SessionAccessor.getServerSessionId(clientSession);
    }

    public void changeServerSessionId(String serverSessionId, boolean skipNullValue, boolean fromServer) {
        if (SessionAccessor.changeServerSessionId(clientSession, serverSessionId, skipNullValue)) {
            serverSessionIdChanged = true;
            if (fromServer) // if the server sent a new session id, it's probably a session loss (the new session is empty),
                forceSendingClientStatesBackToServer(false); // so we need to send the client states again
            else // if the change is from the client,
                nextSessionIdSendingSequence = -1; // we force a resend of the server session id only to the server
            scheduleSessionStoreAndListenerCall();
        }
    }

    public Object getUserId() {
        return SessionAccessor.getUserId(clientSession);
    }

    public void changeUserId(Object userId, boolean skipNullValue, boolean fromServer) {
        if (SessionAccessor.changeUserId(clientSession, userId, skipNullValue)) {
            userIdChanged = true;
            if (!fromServer)
                nextUserIdSendingSequence = -1; // forcing a resend of the user id to the server
            // Erasing userId from the client session if logged out
            if (LogoutUserId.isLogoutUserId(userId)) {
                SessionAccessor.changeUserId(clientSession, null, false);
                // And the token with it, which is not optional. A valid token OVERRIDES the claimed user id on
                // the server, so a client that kept one while announcing a logout would announce the logout and
                // be signed straight back in by its own next message — a logout that does not log out, on the
                // one path where the user has explicitly asked to be signed out.
                SessionAccessor.changeUserToken(clientSession, null, false);
            }
            scheduleSessionStoreAndListenerCall();
        }
    }

    public void setTokenRequired(Boolean tokenRequired) {
        if (tokenRequired != null)
            this.tokenRequired = tokenRequired;
    }

    public String getUserToken() {
        return SessionAccessor.getUserToken(clientSession);
    }

    /**
     * Stores the server's signed statement of who this user is. Only the server ever produces one, so unlike
     * the user id there is no client-originated case: we cannot make one, read one, or alter one without it
     * ceasing to verify. It is kept in the client session so it survives a restart exactly as the user id
     * does — otherwise every relaunch would present an identity with no proof of it.
     */
    public void changeUserToken(String userToken, boolean skipNullValue) {
        if (SessionAccessor.changeUserToken(clientSession, userToken, skipNullValue)) {
            // Persist only — no listener call and no sending-sequence reset, unlike every other change here.
            // Nothing observes the token (there is nothing a UI could usefully do with it), and it goes out on
            // every message anyway, so notifying would schedule a hop to the UI thread to run an empty callback.
            scheduleSessionStorage();
        }
    }

    public String getRunId() {
        return RUN_ID;
    }

    public void changeRunId(String runId, boolean skipNullValue) { // Always called on client side
        if (SessionAccessor.changeRunId(clientSession, runId, skipNullValue)) {
            //runIdChanged = true;
            //lastRunIdSyncedValue = runId;
            scheduleSessionStoreAndListenerCall();
        }
    }

    public Boolean isBackoffice() {
        return BACKOFFICE;
    }

    public boolean isConnected() {
        return connected;
    }

    public void changeConnected(boolean connected) {
        if (connected != this.connected) {
            this.connected = connected;
            connectedChanged = true;
            scheduleListenerCall();
            if (!connected) {
                // forcing a resend of all client states to the server
                forceSendingClientStatesBackToServer(true);
            }
        }
    }

    private void forceSendingClientStatesBackToServer(boolean includingSessionId) {
        nextUserIdSendingSequence =
            nextRunIdSendingSequence =
                nextBackofficeSendingSequence =
                    -1;
        if (includingSessionId)
            nextSessionIdSendingSequence = -1;
    }

    // The following methods are called by ClientSideStateSessionSyncer.syncOutgoingState() and so this is where we
    // eventually complete the outgoing client state sent to the server with missing or changed information.

    // Communicating the server session id to the server (when it makes sense)

    private int nextSessionIdSendingSequence = -1; // setting this to -1 will cause the server session id to be resent

    public Object setOutgoingServerSessionIdIfNotYetSent(Object outgoingState) {
        // When do we send the server session id stored in the client session back to the server?
        if (nextSessionIdSendingSequence == -1) {
            nextSessionIdSendingSequence = serverIncomingMessageSequence;
        }
        if (nextSessionIdSendingSequence == serverIncomingMessageSequence) {
            String serverSessionId = SessionAccessor.getServerSessionId(clientSession);
            outgoingState = StateAccessor.setServerSessionId(outgoingState, serverSessionId, false);
        }
        return outgoingState;
    }

    // Communicating the user id to the server (when it makes sense)

    private int nextUserIdSendingSequence = -1;

    public Object setOutgoingUserIdIfNotYetSent(Object outgoingState) {
        // Not at all, once the server requires a token and we hold one: the token already names us, and a
        // user id beside it is the forgeable half of a pair that can disagree. Note the guard needs the
        // token to be PRESENT, not merely required — a client that has not got one yet must still be able
        // to say who it is, or it could never log in.
        if (tokenRequired && getUserToken() != null)
            return outgoingState;
        // When do we send the user id stored in the client session back to the server?
        if (nextUserIdSendingSequence == -1) {
            nextUserIdSendingSequence = serverIncomingMessageSequence;
        }
        if (nextUserIdSendingSequence == serverIncomingMessageSequence) {
            Object userId = SessionAccessor.getUserId(clientSession);
            outgoingState = StateAccessor.setUserId(outgoingState, userId, false);
        }
        return outgoingState;
    }

    // Communicating the identity token to the server — ALWAYS, unlike everything else in this section.

    /**
     * Puts the token on every outgoing message, with none of the "if not yet sent" machinery its neighbours use.
     *
     * <p>That machinery exists because the server REMEMBERS those values in its session, so resending them is
     * waste. The token is the opposite: it is checked on every message, and it is checked precisely so that the
     * server does not have to take its session's word for who is calling. Sending it only on change would mean
     * almost every message arrives without one — invisible today, since a message with no token is still
     * accepted, and a total loss of login the moment that stops being true. The bug would appear at the flip,
     * in a client nobody had touched, which is the worst possible time to discover it.
     *
     * <p>Being an HMAC, it can afford this: verification is a hash, not a lookup, which is what made checking
     * every message reasonable in the first place.
     */
    public Object setOutgoingUserToken(Object outgoingState) {
        return StateAccessor.setUserToken(outgoingState, getUserToken());
    }

    // Communicating the run id to the server (when it makes sense)
    // Note that the run id is actually not stored in the client session, but is a random constant value on the client side

    private int nextRunIdSendingSequence = -1;

    public Object setOutgoingRunIdIfNotYetSent(Object outgoingState) {
        // When do we send the run to the server?
        if (nextRunIdSendingSequence == -1) {
            nextRunIdSendingSequence = serverIncomingMessageSequence;
        }
        if (nextRunIdSendingSequence == serverIncomingMessageSequence) {
            String runId = getRunId();
            outgoingState = StateAccessor.setRunId(outgoingState, runId, false);
        }
        return outgoingState;
    }

    // Communicating the backoffice flag to the server (when it makes sense)
    // Note that the backoffice flag is actually not stored in the client session, but is a constant value on the client side

    private int nextBackofficeSendingSequence = -1;

    public Object setOutgoingBackofficeIfNotYetSent(Object outgoingState) {
        // When do we send the backoffice flag to the server?
        if (nextBackofficeSendingSequence == -1) {
            nextBackofficeSendingSequence = serverIncomingMessageSequence;
        }
        if (nextRunIdSendingSequence == serverIncomingMessageSequence) {
            Boolean backoffice = isBackoffice();
            outgoingState = StateAccessor.setBackoffice(outgoingState, backoffice, false);
        }
        return outgoingState;
    }

}
