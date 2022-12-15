package dev.webfx.stack.authn.login.spi.impl.server.portal;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.serviceloader.MultipleServiceProviders;
import dev.webfx.stack.authn.login.LoginUiContext;
import dev.webfx.stack.authn.login.spi.LoginServiceProvider;
import dev.webfx.stack.authn.login.spi.impl.server.gateway.ServerLoginGatewayProvider;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public class ServerLoginPortalProvider implements LoginServiceProvider {

    private static List<ServerLoginGatewayProvider> getGatewayProviders() {
        return MultipleServiceProviders.getProviders(ServerLoginGatewayProvider.class, () -> ServiceLoader.load(ServerLoginGatewayProvider.class));
    }

    public ServerLoginPortalProvider() { // Called first time on server start through LoginService.getProvider() call in GetLoginUiInputMethodEndpoint.
        // We instantiate the gateways (such as Google, Facebook, etc...) and call their boot() method, which they will
        // probably use to register their callback route (must be done as soon as possible, i.e. on server start).
        for (ServerLoginGatewayProvider gatewayProvider : getGatewayProviders())
            gatewayProvider.boot();
    }

    @Override
    public Future<?> getLoginUiInput(LoginUiContext loginUiContext) {
        Object gatewayId = loginUiContext.getGatewayId();
        for (ServerLoginGatewayProvider gatewayProvider : getGatewayProviders()) {
            if (Objects.equals(gatewayProvider.getGatewayId(), gatewayId))
                return gatewayProvider.getLoginUiInput(loginUiContext.getGatewayContext());
        }
        return Future.failedFuture("No server login gateway found with id='" + gatewayId + "'");
    }

}
