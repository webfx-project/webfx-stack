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

    private ClientSideStateSessionListener clientSideStateSessionListener;

    public ClientSideStateSession() {
        this(SessionService.getSessionStore());
    }

    public ClientSideStateSession(SessionStore sessionStore) {
        this(sessionStore, sessionStore.createSession());
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
                            Console.log(ar.cause());
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
        // the UI thread, we run it immediately because 1) there is no danger of UI thread exception at this point, and
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
            if (!fromServer)
                nextSessionIdSendingSequence = -1; // forcing a resend of the server session id to the server
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
            // Erasing userId from client session if logged out
            if (LogoutUserId.isLogoutUserId(userId))
                SessionAccessor.changeUserId(clientSession, null, false);
            scheduleSessionStoreAndListenerCall();
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
                // forcing a resend of the server session id to the server
                nextSessionIdSendingSequence =
                    nextUserIdSendingSequence =
                        nextRunIdSendingSequence =
                            nextBackofficeSendingSequence =
                                -1;
            }
        }
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
