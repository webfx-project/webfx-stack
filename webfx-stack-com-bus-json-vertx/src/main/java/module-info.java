// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus.json.vertx {

    // Direct dependencies modules
    requires io.vertx.core;
    requires io.vertx.eventbusbridge;
    requires io.vertx.web;
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.conf;
    requires webfx.platform.util.vertx;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.bus.client;
    requires webfx.stack.com.bus.json;
    requires webfx.stack.com.bus.json.server;
    requires webfx.stack.session;
    requires webfx.stack.session.state;

    // Exported packages
    exports dev.webfx.stack.com.bus.spi.impl.json.vertx;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.com.bus.spi.impl.json.vertx.VertxBusModuleBooter;
    provides dev.webfx.stack.com.bus.spi.BusServiceProvider with dev.webfx.stack.com.bus.spi.impl.json.vertx.VertxBusServiceProvider;

}