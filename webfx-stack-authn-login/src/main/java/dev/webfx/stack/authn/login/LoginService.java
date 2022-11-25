package dev.webfx.stack.authn.login;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;
import dev.webfx.stack.authn.login.spi.LoginServiceProvider;

import java.util.ServiceLoader;


/**
 * @author Bruno Salmon
 */
public final class LoginService {

    public static Future<?> getLoginUiInput() {
        return getProvider().getLoginUiInput();
    }

    public static LoginServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(LoginServiceProvider.class, () -> ServiceLoader.load(LoginServiceProvider.class));
    }

}
