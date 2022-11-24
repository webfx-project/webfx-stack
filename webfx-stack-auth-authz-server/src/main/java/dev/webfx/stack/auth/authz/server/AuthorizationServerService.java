package dev.webfx.stack.auth.authz.server;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;
import dev.webfx.stack.auth.authz.server.spi.AuthorizationServerServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class AuthorizationServerService {

    public static Future<Void> pushAuthorizations(Object userId, Object runId) {
        return getProvider().pushAuthorizations(userId, runId);
    }

    public static AuthorizationServerServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(AuthorizationServerServiceProvider.class, () -> ServiceLoader.load(AuthorizationServerServiceProvider.class));
    }

}
