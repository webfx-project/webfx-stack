// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus.vertx {

    // Direct dependencies modules
    requires io.vertx.core;
    requires webfx.platform.json;
    requires webfx.stack.async;
    requires webfx.stack.com.bus;
    requires webfx.stack.vertx.common;

    // Exported packages
    exports dev.webfx.stack.com.bus.spi.impl.vertx;

    // Provided services
    provides dev.webfx.stack.com.bus.spi.BusServiceProvider with dev.webfx.stack.com.bus.spi.impl.vertx.VertxBusServiceProvider;

}