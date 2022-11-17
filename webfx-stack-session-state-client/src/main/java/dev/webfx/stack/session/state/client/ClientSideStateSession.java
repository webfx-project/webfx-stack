package dev.webfx.stack.session.state.client;

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

    private static final String LOCAL_SESSION_ID = "localSessionId";
    private static final String RUN_ID = Uuid.randomUuid();
    private static final ClientSideStateSession INSTANCE = new ClientSideStateSession();

    public static ClientSideStateSession getInstance() {
        return INSTANCE;
    }

    private final SessionStore sessionStore;
    private Session clientSession;
    private boolean clientSessionChanged;
    private boolean sessionIdChanged;
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
        String clientSessionId = LocalStorage.getItem(LOCAL_SESSION_ID);
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
        setClientSession(clientSession);
    }

    public void incrementServerMessageSequence() {
        serverMessageSequence++;
    }

    private void scheduleSessionStoreAndListenerCall() {
        scheduleSessionStorage();
        scheduleListenerCall();
    }

    private void scheduleSessionStorage() {
        if (scheduledSessionStore != null)
            UiScheduler.scheduleDeferred(scheduledSessionStore = () -> {
                sessionStore.put(clientSession);
                scheduledSessionStore = null;
            });
    }

    private void scheduleListenerCall() {
        if (scheduledListenerCall == null)
            UiScheduler.scheduleDeferred(scheduledListenerCall = () -> {
                callListener();
                scheduledListenerCall = null;
            });
    }

    private void callListener() {
        UiScheduler.runInUiThread(() -> {
            ClientSideStateSessionListener listener = clientSideStateSessionListener;
            if (listener != null) {
                if (clientSessionChanged) {
                    clientSessionChanged = false;
                    listener.onClientSessionChanged(getClientSession());
                }
                if (sessionIdChanged) {
                    sessionIdChanged = false;
                    listener.onSessionIdChanged(getSessionId());
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
            LocalStorage.setItem(LOCAL_SESSION_ID, clientSession.id());
            clientSessionChanged = true;
            scheduleSessionStoreAndListenerCall();
        }
    }

    public String getSessionId() {
        return SessionAccessor.getSessionId(clientSession);
    }

    public void changeSessionId(String sessionId, boolean skipNullValue, boolean fromServer) {
        if (SessionAccessor.changeSessionId(clientSession, sessionId, skipNullValue)) {
            sessionIdChanged = true;
            lastSessionIdSyncedValue = sessionId;
            lastSessionIdSyncedFromServer = fromServer;
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
                lastSessionIdSyncedValue = lastUserIdSyncedValue = lastRunIdSyncedValue = null;
        }
    }


    private String lastSessionIdSyncedValue;
    private boolean lastSessionIdSyncedFromServer;
    private int lastSessionIdSyncedMessageSequence;

    public Object updateStateSessionIdFromClientSessionIfNotYetSynced(Object clientState) {
        String sessionId = SessionAccessor.getSessionId(clientSession);
        if (!Objects.equals(sessionId, lastSessionIdSyncedValue) || !lastSessionIdSyncedFromServer && serverMessageSequence == lastSessionIdSyncedMessageSequence) {
            clientState = StateAccessor.setSessionId(clientState, sessionId, false);
            lastSessionIdSyncedValue = sessionId;
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
