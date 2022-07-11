package dev.webfx.stack.com.bus.spi.impl.vertx;

import io.vertx.core.eventbus.EventBus;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusFactory;
import dev.webfx.stack.com.bus.BusOptions;

/**
 * @author Bruno Salmon
 */
final class VertxBusFactory implements BusFactory {

    private final EventBus eventBus;

    VertxBusFactory(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Bus createBus(BusOptions options) {
        return new VertxBus(eventBus, options);
    }
}
