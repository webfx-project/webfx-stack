package dev.webfx.stack.com.bus.spi.impl.json.client.websocket;

import dev.webfx.stack.com.bus.BusFactory;
import dev.webfx.stack.com.bus.BusOptions;
import dev.webfx.stack.com.bus.spi.impl.client.BusServiceProviderBase;

/**
 * @author Bruno Salmon
 */
public class WebsocketBusServiceProvider extends BusServiceProviderBase {

    @Override
    public BusFactory busFactory() {
        return ReconnectBus::new;
    }

}
