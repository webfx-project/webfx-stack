package dev.webfx.stack.authn.spi.impl.server;

import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.session.state.server.ServerSideStateSessionSyncer;

/**
 * @author Bruno Salmon
 */
public class ServerAuthenticationJob implements ApplicationJob {

    @Override
    public void onStart() {
        ServerSideStateSessionSyncer.setUserIdChecker(userId -> AuthenticationService.verifyAuthenticated().map(x -> (Object) x));
    }
}
