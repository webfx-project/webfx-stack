package dev.webfx.stack.session.state.client.fx;

import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import dev.webfx.stack.session.state.client.ClientSideStateSessionListener;

/**
 * @author Bruno Salmon
 */
final class FxInit {

    static {
        ClientSideStateSession.getInstance().setClientSideStateSessionHolder(new ClientSideStateSessionListener() {

            @Override
            public void onClientSessionChanged(Session clientSession) {
                FxClientSession.setClientSession(clientSession);
            }

            @Override
            public void onSessionIdChanged(String sessionId) {
                FxSessionId.setClientSessionId(sessionId);
            }

            @Override
            public void onUserIdChanged(String userId) {
                FxClientUserId.setClientUserId(userId);
            }

            @Override
            public void onRunIdChanged(String runId) {
                FxClientRunId.setClientRunId(runId);
            }

            @Override
            public void onConnectedChanged(boolean connected) {
                FxClientConnected.setClientConnected(connected);
            }
        });
    }

    static void init() {
    }

}
