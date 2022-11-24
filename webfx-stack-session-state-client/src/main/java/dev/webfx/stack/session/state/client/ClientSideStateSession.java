package dev.webfx.stack.session.state.client;

import dev.webfx.kit.launcher.WebFxKitLauncher;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.storage.LocalStorage;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.util.uuid.Uuid;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.SessionStore;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class ClientSideStateSession {

    private static final String ACTIVE_CLIENT_SESSION_ID = "activeClientSessionId";
    private static final String RUN_ID = Uuid.randomUuid();
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
    private int serverMessageSequence;

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

    public void incrementServerMessageSequence() {
        serverMessageSequence++;
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
            lastServerSessionIdSyncedValue = serverSessionId;
            lastServerSessionIdSyncedFromServer = fromServer;
            scheduleSessionStoreAndListenerCall();
        }
    }

    public String getUserId() {
        return SessionAccessor.getUserId(clientSession);
    }

    public void changeUserId(String userId, boolean skipNullValue, boolean fromServer) {
        if (SessionAccessor.changeUserId(clientSession, userId, skipNullValue)) {
            userIdChanged = true;
            lastUserIdSyncedValue = userId;
            lastUserIdSyncedFromServer = fromServer;
            scheduleSessionStoreAndListenerCall();
        }
    }

    public String getRunId() {
        return RUN_ID;
    }

    public void changeRunId(String runId, boolean skipNullValue, boolean fromServer) {
        if (SessionAccessor.changeRunId(clientSession, runId, skipNullValue)) {
            runIdChanged = true;
            lastRunIdSyncedValue = runId;
            lastRunIdSyncedFromServer = fromServer;
            scheduleSessionStoreAndListenerCall();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void changeConnected(boolean connected) {
        if (connected != this.connected) {
            this.connected = connected;
            connectedChanged = true;
            scheduleListenerCall();
            if (!connected)
                lastServerSessionIdSyncedValue = lastUserIdSyncedValue = lastRunIdSyncedValue = null;
        }
    }


    private String lastServerSessionIdSyncedValue;
    private boolean lastServerSessionIdSyncedFromServer;
    private int lastSessionIdSyncedMessageSequence;

    public Object updateStateServerSessionIdFromClientSessionIfNotYetSynced(Object clientState) {
        String serverSessionId = SessionAccessor.getServerSessionId(clientSession);
        if (!Objects.equals(serverSessionId, lastServerSessionIdSyncedValue) || !lastServerSessionIdSyncedFromServer && serverMessageSequence == lastSessionIdSyncedMessageSequence) {
            clientState = StateAccessor.setServerSessionId(clientState, serverSessionId, false);
            lastServerSessionIdSyncedValue = serverSessionId;
            lastSessionIdSyncedMessageSequence = serverMessageSequence;
        }
        return clientState;
    }

    private String lastUserIdSyncedValue;
    private boolean lastUserIdSyncedFromServer;
    private int lastUserIdSyncedMessageSequence;

    public Object updateStateUserIdFromClientSessionIfNotYetSynced(Object clientState) {
        String userId = SessionAccessor.getUserId(clientSession);
        if (!Objects.equals(userId, lastUserIdSyncedValue) || !lastUserIdSyncedFromServer && serverMessageSequence == lastUserIdSyncedMessageSequence) {
            clientState = StateAccessor.setUserId(clientState, userId, false);
            lastUserIdSyncedValue = userId;
            lastUserIdSyncedMessageSequence = serverMessageSequence;
        }
        return clientState;
    }

    private String lastRunIdSyncedValue;
    private boolean lastRunIdSyncedFromServer;
    private int lastRunIdSyncedMessageSequence;

    public Object updateStateRunIdFromClientSessionIfNotYetSynced(Object clientState) {
        String runId = getRunId();
        if (!Objects.equals(runId, lastRunIdSyncedValue) || !lastRunIdSyncedFromServer && serverMessageSequence == lastRunIdSyncedMessageSequence) {
            clientState = StateAccessor.setRunId(clientState, runId, false);
            lastRunIdSyncedValue = runId;
            lastRunIdSyncedMessageSequence = serverMessageSequence;
        }
        return clientState;
    }

}
