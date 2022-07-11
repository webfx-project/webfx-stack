package dev.webfx.stack.com.bus.spi.impl.simple;

import dev.webfx.stack.com.bus.BusFactory;
import dev.webfx.stack.com.bus.BusOptions;
import dev.webfx.stack.com.bus.spi.impl.BusServiceProviderBase;
import dev.webfx.stack.com.bus.spi.impl.SimpleBus;

public class SimpleBusServiceProvider extends BusServiceProviderBase {

    @Override
    public BusFactory busFactory() {
        return o -> new SimpleBus();
    }

    @Override
    public BusOptions createBusOptions() {
        return null;
    }

    @Override
    public void setPlatformBusOptions(BusOptions options) {

    }
}
