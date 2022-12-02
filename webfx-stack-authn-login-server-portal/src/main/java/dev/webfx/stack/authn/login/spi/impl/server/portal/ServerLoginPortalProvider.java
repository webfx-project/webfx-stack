package dev.webfx.stack.authn.login.spi.impl.server.portal;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.login.spi.LoginServiceProvider;
import dev.webfx.stack.authn.login.spi.impl.server.gateway.ServerLoginGatewayProvider;

import java.util.Objects;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public class ServerLoginPortalProvider implements LoginServiceProvider {

    @Override
    public Future<?> getLoginUiInput(Object gatewayId) {
        for (ServerLoginGatewayProvider gatewayProvider : getGatewayProviders()) {
            if (Objects.equals(gatewayProvider.getGatewayId(), gatewayId))
                return gatewayProvider.getLoginUiInput();
        }
        return Future.failedFuture("No server login gateway found with id='" + gatewayId + "'");
    }

    private ServiceLoader<ServerLoginGatewayProvider> getGatewayProviders() {
        return ServiceLoader.load(ServerLoginGatewayProvider.class);
    }

}
