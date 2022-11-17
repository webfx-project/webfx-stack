package dev.webfx.stack.session.state.client;

import dev.webfx.stack.session.Session;

public interface ClientSideStateSessionListener {

    void onClientSessionChanged(Session clientSession);

    void onSessionIdChanged(String sessionId);

    void onUserIdChanged(String userId);

    void onRunIdChanged(String runId);

    void onConnectedChanged(boolean connected);


}
