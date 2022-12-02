package dev.webfx.stack.com.bus.spi;

import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusFactory;
import dev.webfx.stack.com.bus.BusOptions;
import dev.webfx.stack.com.bus.spi.impl.BusOptionsConfigurationConsumer;

/**
 * @author Bruno Salmon
 */
public interface BusServiceProvider {

    BusFactory busFactory();

    default BusOptions createBusOptions() { return new BusOptions();}

    default void setPlatformBusOptions(BusOptions options) {
        options.turnUnsetPropertiesToDefault();
        ReadOnlyKeyObject config = BusOptionsConfigurationConsumer.BUS_OPTIONS_CONFIGURATION;
        if (config != null)
            options.applyConfig(config);
    }

    Bus bus();

    BusOptions getBusOptions();

    void setBusOptions(BusOptions busOptions);

    Bus createBus();

    Bus createBus(BusOptions options);

}
