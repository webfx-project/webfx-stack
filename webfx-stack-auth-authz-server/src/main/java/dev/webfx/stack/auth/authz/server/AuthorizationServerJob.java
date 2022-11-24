package dev.webfx.stack.auth.authz.server;

import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.stack.session.state.server.ServerSideStateSessionSyncer;

/**
 * @author Bruno Salmon
 */
public class AuthorizationServerJob implements ApplicationJob {

    @Override
    public void onStart() {
        ServerSideStateSessionSyncer.setUserIdAuthorizer(AuthorizationServerService::pushAuthorizations);
    }
}
