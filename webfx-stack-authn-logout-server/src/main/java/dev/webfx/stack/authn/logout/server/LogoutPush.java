package dev.webfx.stack.authn.logout.server;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import dev.webfx.stack.push.server.PushServerService;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.LogoutUserId;

/**
 * @author Bruno Salmon
 */
public class LogoutPush {

    public static Future<Void> pushLogoutMessageToClient() {
        String runId = ThreadLocalStateHolder.getRunId();
        return PushServerService.pushState(StateAccessor.createUserIdState(LogoutUserId.LOGOUT_USER_ID), runId);
    }
}
