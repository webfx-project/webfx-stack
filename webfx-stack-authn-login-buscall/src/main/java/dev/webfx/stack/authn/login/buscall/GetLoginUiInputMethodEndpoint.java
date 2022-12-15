package dev.webfx.stack.authn.login.buscall;

import dev.webfx.stack.authn.login.LoginService;
import dev.webfx.stack.authn.login.LoginUiContext;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class GetLoginUiInputMethodEndpoint extends AsyncFunctionBusCallEndpoint<LoginUiContext, Object> {

    public GetLoginUiInputMethodEndpoint() {
        super(LoginServiceBusAddress.GET_LOGIN_UI_INPUT_METHOD_ADDRESS, loginUiContext ->
                LoginService.getLoginUiInput(loginUiContext).map(x -> (Object) x));
        // We take the opportunity of this call to also instantiate the Login provider right now, i.e. even before the
        // first client bus call. This is important on server side, because this will instantiate the login portal (as
        // ServerLoginPortalProvider is the login provider on server side), which will instantiate (if you look at the
        // ServerLoginPortalProvider() constructor) the registered login gateways (such as Google, Facebook, etc...),
        // and call their boot() method, which will register their callback route (such as /login/google/callback).
        // If we don't do it right now on server start, it will be done later on first client login request, but this
        // can be too late for all possible users already engaged in the login process just before the server restart
        // (the callback route will not work for them).
        LoginService.getProvider();
    }
}
