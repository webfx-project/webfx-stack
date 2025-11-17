package dev.webfx.stack.authz.client;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.authz.client.spi.AuthorizationClientServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class AuthorizationClientService {

    public static Future<Boolean> isAuthorized(Object operationAuthorizationRequest) {
        return getProvider().isAuthorized(operationAuthorizationRequest);
    }

    public static AuthorizationClientServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(AuthorizationClientServiceProvider.class, () -> ServiceLoader.load(AuthorizationClientServiceProvider.class));
    }
}
