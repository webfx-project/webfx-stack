package dev.webfx.stack.com.bus.spi.impl.json.vertx;

import dev.webfx.stack.com.bus.BusFactory;
import dev.webfx.platform.util.vertx.VertxInstance;
import dev.webfx.stack.com.bus.spi.impl.client.BusServiceProviderBase;

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
