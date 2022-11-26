package dev.webfx.stack.authn.logout.client.operation;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.AuthenticationService;

/**
 * @author Bruno Salmon
 */
public final class LogoutExecutor {

    static Future<Void> executeRequest(LogoutRequest rq) {
        return execute();
    }

    private static Future<Void> execute() {
        return AuthenticationService.logout();
    }

}
