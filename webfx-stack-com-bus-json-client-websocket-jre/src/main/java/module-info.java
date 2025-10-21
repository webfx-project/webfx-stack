// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus.json.client.websocket.jre {

    // Direct dependencies modules
    requires webfx.stack.com.bus;
    requires webfx.stack.com.bus.json.client.websocket;

    // Exported packages
    exports dev.webfx.stack.com.bus.spi.impl.json.client.websocket.jre;

    // Provided services
    provides dev.webfx.stack.com.bus.spi.BusServiceProvider with dev.webfx.stack.com.bus.spi.impl.json.client.websocket.jre.JreWebsocketBusServiceProvider;

}