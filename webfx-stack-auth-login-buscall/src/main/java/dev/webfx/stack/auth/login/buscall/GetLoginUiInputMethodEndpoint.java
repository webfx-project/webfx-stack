package dev.webfx.stack.auth.login.buscall;

import dev.webfx.stack.auth.login.LoginService;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class GetLoginUiInputMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, Object> {

    public GetLoginUiInputMethodEndpoint() {
        super(LoginServiceBusAddress.GET_LOGIN_UI_INPUT_METHOD_ADDRESS, ignored -> LoginService.getLoginUiInput().map(x -> (Object) x));
    }
}