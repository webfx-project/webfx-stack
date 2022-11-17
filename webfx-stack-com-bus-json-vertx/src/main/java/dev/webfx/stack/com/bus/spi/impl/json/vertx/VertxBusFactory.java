package dev.webfx.stack.com.bus.spi.impl.json.vertx;

import io.vertx.core.eventbus.EventBus;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusFactory;
import dev.webfx.stack.com.bus.BusOptions;

/**
 * @author Bruno Salmon
 */
final class VertxBusFactory implements BusFactory {

    private final Bus bus;

    VertxBusFactory(EventBus eventBus) {
        bus = new VertxBus(eventBus);
    }

    @Override
    public Bus createBus(BusOptions options) {
        return bus;
    }
}
