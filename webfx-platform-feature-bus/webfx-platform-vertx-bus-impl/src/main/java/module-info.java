// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.vertx.bus.impl {

    // Direct dependencies modules
    requires io.vertx.core;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.bus;
    requires webfx.platform.shared.json;
    requires webfx.platform.vertx.instance;

    // Exported packages
    exports dev.webfx.stack.platform.vertx.services.bus.spi.impl;

    // Provided services
    provides dev.webfx.stack.platform.shared.services.bus.spi.BusServiceProvider with dev.webfx.stack.platform.vertx.services.bus.spi.impl.VertxBusServiceProvider;

}