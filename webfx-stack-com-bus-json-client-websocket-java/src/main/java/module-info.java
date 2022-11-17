// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus.json.client.websocket.java {

    // Direct dependencies modules
    requires webfx.platform.windowlocation;
    requires webfx.platform.windowlocation.java;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.bus.json.client.websocket;

    // Exported packages
    exports dev.webfx.stack.com.bus.spi.impl.json.client.websocket.java;

    // Provided services
    provides dev.webfx.stack.com.bus.spi.BusServiceProvider with dev.webfx.stack.com.bus.spi.impl.json.client.websocket.java.JavaWebsocketBusServiceProvider;

}