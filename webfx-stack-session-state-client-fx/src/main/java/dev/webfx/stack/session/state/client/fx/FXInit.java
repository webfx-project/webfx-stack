package dev.webfx.stack.session.state.client.fx;

import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import dev.webfx.stack.session.state.client.ClientSideStateSessionListener;

/**
 * It's important to call FXInit.init() first whatever FXClass the application code starts using. FXInit will install
 * all necessary listeners over all FXClasses present in this package, so that the additional listeners possibly set by
 * the application code are positioned after. This ensures that when they are called, all FX classes have already
 * transitioned to a final, coherent & stable state.
 *
 * @author Bruno Salmon
 */
final class FXInit {

    static void init() { } // Only the first call will trigger the static initializer below, not subsequent calls

    static {
        FXLoggedOut.init();
        FXAuthorizationsWaiting.init();
        FXAuthorizationsReceived.init();
        FXLoggedIn.init();
        FXConnectionSequence.init();

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

}
