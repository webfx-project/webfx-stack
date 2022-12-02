package dev.webfx.stack.session.state.client.fx;

import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import dev.webfx.stack.session.state.client.ClientSideStateSessionListener;

/**
 * @author Bruno Salmon
 */
final class FXInit {

    static {
        ClientSideStateSession.getInstance().setClientSideStateSessionHolder(new ClientSideStateSessionListener() {

            @Override
            public void onClientSessionChanged(Session clientSession) {
                FXSession.setSession(clientSession);
            }

            @Override
            public void onServerSessionIdChanged(String serverSessionId) {
                FXServerSessionId.setServerSessionId(serverSessionId);
            }

            @Override
            public void onUserIdChanged(Object userId) {
                FXUserId.setUserId(userId);
            }

            @Override
            public void onRunIdChanged(String runId) {
                FXRunId.setRunId(runId);
            }

            @Override
            public void onConnectedChanged(boolean connected) {
                FXConnected.setConnected(connected);
            }
        });
    }

    static void init() {
    }

}
