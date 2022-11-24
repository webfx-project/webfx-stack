package dev.webfx.stack.auth.authn;

import dev.webfx.stack.auth.authn.spi.AuthenticationServiceProvider;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class AuthenticationService {

    public static Future<?> authenticate(Object userCredentials) {
        return getProvider().authenticate(userCredentials);
    }

    public static Future<?> verifyAuthenticated(Object userId) {
        return getProvider().verifyAuthenticated(userId);
    }

    public static Future<UserClaims> getUserClaims(Object userId) {
        return getProvider().getUserClaims(userId);
    }

    public static AuthenticationServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(AuthenticationServiceProvider.class, () -> ServiceLoader.load(AuthenticationServiceProvider.class));
    }
}
