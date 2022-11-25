package dev.webfx.stack.authn.login.spi;

import dev.webfx.platform.async.Future;

public interface LoginServiceProvider {

    Future<?> getLoginUiInput();

}
