package dev.webfx.stack.com.bus.spi.impl.simple;

import dev.webfx.stack.com.bus.BusFactory;
import dev.webfx.stack.com.bus.BusOptions;
import dev.webfx.stack.com.bus.spi.impl.client.BusServiceProviderBase;
import dev.webfx.stack.com.bus.spi.impl.client.SimpleBus;

public class SimpleBusServiceProvider extends BusServiceProviderBase {

    @Override
    public BusFactory busFactory() {
        return SimpleBus::new;
    }

}
