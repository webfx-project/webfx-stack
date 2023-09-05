package dev.webfx.stack.com.bus.spi;

import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusFactory;
import dev.webfx.stack.com.bus.BusOptions;

/**
 * @author Bruno Salmon
 */
public interface BusServiceProvider {

    BusFactory busFactory();

    Bus bus();

    Bus createBus();

}
