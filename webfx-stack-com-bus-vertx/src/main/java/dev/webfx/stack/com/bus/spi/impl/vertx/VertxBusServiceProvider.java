package dev.webfx.stack.com.bus.spi.impl.vertx;

import dev.webfx.stack.com.bus.BusFactory;
import dev.webfx.stack.vertx.common.VertxInstance;
import dev.webfx.stack.com.bus.spi.impl.BusServiceProviderBase;

/**
 * @author Bruno Salmon
 */
public final class VertxBusServiceProvider extends BusServiceProviderBase {

    private final BusFactory vertxBusFactory;

    public VertxBusServiceProvider() {
        vertxBusFactory = new VertxBusFactory(VertxInstance.getVertx().eventBus());
    }

    @Override
    public BusFactory busFactory() {
        return vertxBusFactory;
    }
}
