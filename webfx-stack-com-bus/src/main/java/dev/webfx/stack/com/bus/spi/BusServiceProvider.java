package dev.webfx.stack.com.bus.spi;

import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusFactory;
import dev.webfx.stack.com.bus.BusOptions;

/**
 * @author Bruno Salmon
 */
public interface BusServiceProvider {

    BusFactory busFactory();

    default BusOptions createBusOptions() { return new BusOptions();}

    default void setPlatformBusOptions(BusOptions options) {
        options.turnUnsetPropertiesToDefault();
    }

    Bus bus();

    BusOptions getBusOptions();

    void setBusOptions(BusOptions busOptions);

    Bus createBus();

    Bus createBus(BusOptions options);

}
