// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus.json.vertx {

    // Direct dependencies modules
    requires io.vertx.core;
    requires io.vertx.eventbusbridge.common;
    requires io.vertx.web;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.conf;
    requires webfx.platform.vertx.common;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.bus.client;
    requires webfx.stack.com.bus.json;
    requires webfx.stack.com.bus.json.server;
    requires webfx.stack.session.state;
    requires webfx.stack.session.vertx;

    // Exported packages
    exports dev.webfx.stack.com.bus.spi.impl.json.vertx;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.com.bus.spi.impl.json.vertx.VertxBusModuleBooter;
    provides dev.webfx.stack.com.bus.spi.BusServiceProvider with dev.webfx.stack.com.bus.spi.impl.json.vertx.VertxBusServiceProvider;

}