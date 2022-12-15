package dev.webfx.stack.authn.login.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.login.LoginUiContext;

public interface LoginServiceProvider {

    Future<?> getLoginUiInput(LoginUiContext loginUiContext);

}
