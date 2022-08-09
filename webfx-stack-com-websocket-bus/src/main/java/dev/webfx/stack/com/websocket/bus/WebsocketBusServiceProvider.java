package dev.webfx.stack.com.websocket.bus;

import dev.webfx.stack.com.bus.BusFactory;
import dev.webfx.stack.com.bus.BusOptions;
import dev.webfx.stack.com.bus.spi.impl.BusServiceProviderBase;
import dev.webfx.platform.resource.Resource;
import dev.webfx.platform.json.Json;

/**
 * @author Bruno Salmon
 */
public class WebsocketBusServiceProvider extends BusServiceProviderBase {

    @Override
    public BusFactory busFactory() {
        return ReconnectBus::new;
    }

    @Override
    public BusOptions createBusOptions() {
        return new WebSocketBusOptions();
    }

    @Override
    public void setPlatformBusOptions(BusOptions options) {
        super.setPlatformBusOptions(options);
        String json = Resource.getText("dev/webfx/stack/com/websocket/bus/conf/BusOptions.json");
        if (json != null)
            options.applyJson(Json.parseObject(json));
    }

}
