package dev.webfx.stack.platform.windowlocation.spi.impl.java;

import dev.webfx.stack.platform.windowhistory.WindowHistory;
import dev.webfx.stack.platform.windowlocation.spi.WindowLocationProvider;
import dev.webfx.stack.platform.windowlocation.spi.impl.BrowsingLocationImpl;
import dev.webfx.stack.com.websocketbus.WebSocketBusOptions;
import dev.webfx.stack.com.bus.BusService;

/**
 * @author Bruno Salmon
 */
public final class JavaWindowLocationProvider extends BrowsingLocationImpl implements WindowLocationProvider {

    public JavaWindowLocationProvider() {
        super(((WebSocketBusOptions) BusService.getBusOptions()).isServerSSL() ? "https" : "http", ((WebSocketBusOptions) BusService.getBusOptions()).getServerHost(), null, WindowHistory.getCurrentLocation());
    }
}
