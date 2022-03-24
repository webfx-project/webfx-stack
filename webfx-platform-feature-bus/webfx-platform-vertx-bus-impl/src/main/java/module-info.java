// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.vertx.bus.impl {

    // Direct dependencies modules
    requires io.vertx.core;
    requires webfx.platform.shared.bus;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.util;
    requires webfx.platform.vertx.instance;

    // Exported packages
    exports dev.webfx.platform.vertx.services.bus.spi.impl;

    // Provided services
    provides dev.webfx.platform.shared.services.bus.spi.BusServiceProvider with dev.webfx.platform.vertx.services.bus.spi.impl.VertxBusServiceProvider;

}