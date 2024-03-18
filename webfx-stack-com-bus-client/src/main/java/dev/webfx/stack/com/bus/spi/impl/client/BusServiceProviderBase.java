package dev.webfx.stack.com.bus.spi.impl.client;

import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.spi.BusServiceProvider;
import dev.webfx.stack.com.bus.ThreadLocalBusContext;

/**
 * @author Bruno Salmon
 */
public abstract class BusServiceProviderBase implements BusServiceProvider {

    private static Bus BUS;
    public Bus bus() {
        Bus bus = ThreadLocalBusContext.getThreadLocalBus();
        if (bus != null)
            return bus;
        if (BUS == null)
            BUS = createBus();
        return BUS;
    }

    public Bus createBus() {
        Bus bus = busFactory().createBus();
        if (BUS == null)
            BUS = bus;
        return bus;
    }

}
