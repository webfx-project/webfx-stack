package dev.webfx.stack.authn.login.buscall;

import dev.webfx.stack.authn.login.LoginService;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class GetLoginUiInputMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, Object> {

    public GetLoginUiInputMethodEndpoint() {
        super(LoginServiceBusAddress.GET_LOGIN_UI_INPUT_METHOD_ADDRESS, context -> LoginService.getLoginUiInput(context).map(x -> (Object) x));
    }
}
