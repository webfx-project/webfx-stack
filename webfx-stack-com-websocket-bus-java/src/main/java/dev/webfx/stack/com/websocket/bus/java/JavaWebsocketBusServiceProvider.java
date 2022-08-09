package dev.webfx.stack.com.websocket.bus.java;

import dev.webfx.stack.com.bus.BusOptions;
import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.com.websocket.bus.WebSocketBusOptions;
import dev.webfx.stack.com.websocket.bus.WebsocketBusServiceProvider;
import dev.webfx.platform.windowlocation.spi.HostLocation;
import dev.webfx.platform.windowlocation.spi.impl.java.JavaWindowLocationProvider;

/**
 * @author Bruno Salmon
 */
public final class JavaWebsocketBusServiceProvider extends WebsocketBusServiceProvider {

    static {
        // Setting the host location for the Java window location
        JavaWindowLocationProvider.setHostLocation(new HostLocation() {

            private WebSocketBusOptions getWebSocketOptions() {
                return (WebSocketBusOptions) BusService.getBusOptions();
            }

            @Override
            public String getProtocol() {
                return getWebSocketOptions().isServerSSL() ? "https" : "http";
            }

            @Override
            public String getHostname() {
                return getWebSocketOptions().getServerHost();
            }

            @Override
            public String getPort() {
                return getWebSocketOptions().getServerPort();
            }
        });
    }

    @Override
    public void setPlatformBusOptions(BusOptions options) {
        super.setPlatformBusOptions(options);
        if (options instanceof WebSocketBusOptions) {
            WebSocketBusOptions webSocketBusOptions = (WebSocketBusOptions) options;
            JavaWindowLocationProvider.setHostLocation(new HostLocation() {
                @Override
                public String getProtocol() {
                    return webSocketBusOptions.isServerSSL() ? "https" : "http";
                }

                @Override
                public String getHostname() {
                    return webSocketBusOptions.getServerHost();
                }

                @Override
                public String getPort() {
                    return webSocketBusOptions.getServerPort();
                }
            });
        }
    }

}
