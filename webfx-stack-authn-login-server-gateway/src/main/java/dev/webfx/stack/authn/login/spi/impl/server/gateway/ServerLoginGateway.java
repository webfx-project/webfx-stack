package dev.webfx.stack.authn.login.spi.impl.server.gateway;

import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface ServerLoginGateway {

    void boot();

    Object getGatewayId();

    Future<?> getLoginUiInput(Object gatewayContext);

}
