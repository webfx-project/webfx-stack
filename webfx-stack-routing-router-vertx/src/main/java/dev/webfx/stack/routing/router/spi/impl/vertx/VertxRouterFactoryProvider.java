package dev.webfx.stack.routing.router.spi.impl.vertx;

import dev.webfx.platform.vertx.common.VertxInstance;
import dev.webfx.stack.routing.router.Router;
import dev.webfx.stack.routing.router.spi.RouterFactoryProvider;

/**
 * @author Bruno Salmon
 */
public final class VertxRouterFactoryProvider implements RouterFactoryProvider {
    @Override
    public Router createRouter() {
        return VertxRouter.create(VertxInstance.getHttpRouter());
    }
}
