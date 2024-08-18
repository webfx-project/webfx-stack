package dev.webfx.stack.authz.server;

import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.state.server.ServerSideStateSessionSyncer;

/**
 * @author Bruno Salmon
 */
public class AuthorizationServerJob implements ApplicationJob {

    @Override
    public void onStart() {
        ServerSideStateSessionSyncer.setUserIdAuthorizer(ignored -> {
            // We push the authorizations associated with the userId to the client (identified by runId). It's important
            // to first set these 2 parameters (userId and runId) in ThreadLocalStateHolder before calling this method.
            // This responsibility is fulfilled by ServerSideStateSessionSyncer.
            return AuthorizationServerService.pushAuthorizations()
                .onFailure(e -> Console.log("⛔️ An error occurred while fetching and/or pushing authorizations to user", e));
        });
    }
}
