// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.client.websocketbus {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.client.websocket;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.bus;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.resource;
    requires webfx.platform.shared.scheduler;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.stack.platform.websocketbus;

    // Provided services
    provides dev.webfx.stack.platform.shared.services.bus.spi.BusServiceProvider with dev.webfx.stack.platform.websocketbus.WebsocketBusServiceProvider;

}