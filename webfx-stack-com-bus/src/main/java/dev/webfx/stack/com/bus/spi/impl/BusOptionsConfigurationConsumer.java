package dev.webfx.stack.com.bus.spi.impl;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.conf.spi.impl.NoDefaultConfigurationConsumerBase;

/**
 * @author Bruno Salmon
 */
public class BusOptionsConfigurationConsumer extends NoDefaultConfigurationConsumerBase {

    private final static String BUS_OPTIONS_CONFIGURATION_NAME = "ClientBusOptions";

    public static ReadOnlyKeyObject BUS_OPTIONS_CONFIGURATION;

    public BusOptionsConfigurationConsumer() {
        super(BUS_OPTIONS_CONFIGURATION_NAME);
    }

    @Override
    public Future<Void> boot() {
        BUS_OPTIONS_CONFIGURATION = readConfiguration();
        return Future.succeededFuture();
    }
}
