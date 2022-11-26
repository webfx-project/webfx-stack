package dev.webfx.stack.authz.server;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;
import dev.webfx.stack.authz.server.spi.AuthorizationServerServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class AuthorizationServerService {

    public static Future<Void> pushAuthorizations() {
        return getProvider().pushAuthorizations();
    }

    public static AuthorizationServerServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(AuthorizationServerServiceProvider.class, () -> ServiceLoader.load(AuthorizationServerServiceProvider.class));
    }

}
