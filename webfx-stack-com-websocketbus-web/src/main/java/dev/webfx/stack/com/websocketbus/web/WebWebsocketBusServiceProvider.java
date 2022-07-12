package dev.webfx.stack.com.websocketbus.web;

import dev.webfx.stack.com.bus.BusOptions;
import dev.webfx.stack.com.websocketbus.WebsocketBusServiceProvider;
import dev.webfx.stack.com.websocketbus.WebSocketBusOptions;
import dev.webfx.stack.platform.json.Json;
import dev.webfx.platform.resource.Resource;
import dev.webfx.stack.platform.windowlocation.WindowLocation;

/**
 * @author Bruno Salmon
 */
public final class WebWebsocketBusServiceProvider extends WebsocketBusServiceProvider {

    @Override
    public void setPlatformBusOptions(BusOptions options) {
        WebSocketBusOptions socketBusOptions = (WebSocketBusOptions) options;
        // Setting protocol to HTTP (unless already explicitly set by the application)
        if (socketBusOptions.getProtocol() == null)
            socketBusOptions.setProtocol(WebSocketBusOptions.Protocol.HTTP);
        // Setting server host from url hostname (if not explicitly set)
        if (socketBusOptions.getServerHost() == null)
            socketBusOptions.setServerHost(WindowLocation.getHostname());
        // Setting server port from url port (if not explicitly set)
        if (socketBusOptions.getServerPort() == null) {
            String port = WindowLocation.getPort();
            if ("63342".equals(port)) // Port used by IntelliJ IDEA to serve web pages when testing directly in IDEA
                port = "80"; // But the actual webfx server web port on the development local machine is 80 in this case
            socketBusOptions.setServerPort(port);
        }
        // Setting server SSL from url protocol (if not explicitly set)
        if (socketBusOptions.isServerSSL() == null)
            socketBusOptions.setServerSSL("https".equals(WindowLocation.getProtocol()));
        super.setPlatformBusOptions(options);
        String json = Resource.getText("dev/webfx/stack/com/websocketbus/conf/BusOptions.json");
        if (json != null)
            options.applyJson(Json.parseObject(json));
    }

}
