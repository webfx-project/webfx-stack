package dev.webfx.stack.auth.login.spi;

import dev.webfx.platform.async.Future;

public interface LoginServiceProvider {

    Future<?> getLoginUiInput();

}
