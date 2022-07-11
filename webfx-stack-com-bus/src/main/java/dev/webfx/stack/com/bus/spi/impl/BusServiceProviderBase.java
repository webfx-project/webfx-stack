package dev.webfx.stack.com.bus.spi.impl;

import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusOptions;
import dev.webfx.stack.com.bus.spi.BusServiceProvider;
import dev.webfx.stack.com.bus.ThreadLocalBusContext;

/**
 * @author Bruno Salmon
 */
public abstract class BusServiceProviderBase implements BusServiceProvider {

    private static Bus BUS;
    private static BusOptions busOptions;

    public Bus bus() {
        Bus bus = ThreadLocalBusContext.getThreadLocalBus();
        if (bus != null)
            return bus;
        if (BUS == null)
            BUS = createBus();
        return BUS;
    }

    public BusOptions getBusOptions() {
        return busOptions;
    }

    public void setBusOptions(BusOptions busOptions) {
        BusServiceProviderBase.busOptions = busOptions;
    }

    public Bus createBus() {
        if (busOptions == null)
            busOptions = createBusOptions();
        return createBus(busOptions);
    }

    public Bus createBus(BusOptions options) {
        setPlatformBusOptions(options);
        Bus bus = busFactory().createBus(options);
        if (BUS == null)
            BUS = bus;
        return bus;
    }

}
