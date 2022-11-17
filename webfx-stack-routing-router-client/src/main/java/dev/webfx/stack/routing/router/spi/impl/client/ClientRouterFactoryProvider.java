package dev.webfx.stack.routing.router.spi.impl.client;

import dev.webfx.stack.routing.router.Router;
import dev.webfx.stack.routing.router.spi.RouterFactoryProvider;

/**
 * @author Bruno Salmon
 */
public final class ClientRouterFactoryProvider implements RouterFactoryProvider {

    @Override
    public Router createRouter() {
        return new ClientRouter();
    }
}
