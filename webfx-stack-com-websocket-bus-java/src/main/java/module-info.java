// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.websocket.bus.java {

    // Direct dependencies modules
    requires webfx.platform.windowlocation;
    requires webfx.platform.windowlocation.java;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.websocket.bus;

    // Exported packages
    exports dev.webfx.stack.com.websocket.bus.java;

    // Provided services
    provides dev.webfx.stack.com.bus.spi.BusServiceProvider with dev.webfx.stack.com.websocket.bus.java.JavaWebsocketBusServiceProvider;

}