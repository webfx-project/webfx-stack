// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.websocketbus {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.resource;
    requires webfx.platform.shared.scheduler;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.websocket;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.com.websocketbus;

    // Provided services
    provides dev.webfx.stack.com.bus.spi.BusServiceProvider with dev.webfx.stack.com.websocketbus.WebsocketBusServiceProvider;

}