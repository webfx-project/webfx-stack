// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.websocketbus {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.console;
    requires webfx.platform.resource;
    requires webfx.platform.scheduler;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.websocket;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.com.websocketbus;

    // Provided services
    provides dev.webfx.stack.com.bus.spi.BusServiceProvider with dev.webfx.stack.com.websocketbus.WebsocketBusServiceProvider;

}