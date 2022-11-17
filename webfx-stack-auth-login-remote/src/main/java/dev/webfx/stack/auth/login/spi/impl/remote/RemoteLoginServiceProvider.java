package dev.webfx.stack.auth.login.spi.impl.remote;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.auth.login.buscall.LoginServiceBusAddress;
import dev.webfx.stack.auth.login.spi.LoginServiceProvider;
import dev.webfx.stack.com.bus.call.BusCallService;

/**
 * @author Bruno Salmon
 */
public class RemoteLoginServiceProvider implements LoginServiceProvider {

    @Override
    public Future<?> getLoginUiInput() {
        return BusCallService.call(LoginServiceBusAddress.GET_LOGIN_UI_INPUT_METHOD_ADDRESS, null);
    }

}
