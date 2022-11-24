package dev.webfx.stack.auth.authn.server;

import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.stack.auth.authn.AuthenticationService;
import dev.webfx.stack.session.state.server.ServerSideStateSessionSyncer;

/**
 * @author Bruno Salmon
 */
public class AuthenticationServerJob implements ApplicationJob {

    @Override
    public void onStart() {
        ServerSideStateSessionSyncer.setUserIdChecker(userId -> AuthenticationService.verifyAuthenticated(userId).map(Object::toString));
    }
}
