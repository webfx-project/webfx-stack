package dev.webfx.stack.authz.client.spi.impl;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authz.client.spi.AuthorizationClientServiceProvider;
import dev.webfx.stack.session.state.client.fx.FXUserId;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public abstract class AuthorizationClientServiceProviderBase implements AuthorizationClientServiceProvider {

    private final Map<Object, UserAuthorizationChecker> cache = new HashMap<>();

    @Override
    public Future<Boolean> isAuthorized(Object operationAuthorizationRequest) {
        return getOrCreateUserAuthorizationChecker().isAuthorized(operationAuthorizationRequest);
    }

    protected UserAuthorizationChecker getOrCreateUserAuthorizationChecker() {
        return getOrCreateUserAuthorizationChecker(FXUserId.getUserId());
    }

    protected UserAuthorizationChecker getOrCreateUserAuthorizationChecker(Object userId) {
        UserAuthorizationChecker userAuthorizationChecker = cache.get(userId);
        if (userAuthorizationChecker == null)
            cache.put(userId, userAuthorizationChecker = createUserAuthorizationChecker());
        return userAuthorizationChecker;
    }

    protected abstract UserAuthorizationChecker createUserAuthorizationChecker();
}
