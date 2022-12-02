// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus.json.vertx {

    // Direct dependencies modules
    requires io.vertx.core;
    requires io.vertx.eventbusbridge.common;
    requires io.vertx.web;
    requires webfx.platform.async;
    requires webfx.platform.json;
    requires webfx.platform.util;
    requires webfx.platform.vertx.common;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.bus.json;
    requires webfx.stack.com.bus.json.server;
    requires webfx.stack.conf;
    requires webfx.stack.conf.resource;
    requires webfx.stack.session.state;
    requires webfx.stack.session.vertx;

    // Exported packages
    exports dev.webfx.stack.com.bus.spi.impl.json.vertx;

    // Resources packages
    opens dev.webfx.stack.com.bus.spi.impl.json.vertx;

    // Provided services
    provides dev.webfx.stack.com.bus.spi.BusServiceProvider with dev.webfx.stack.com.bus.spi.impl.json.vertx.VertxBusServiceProvider;
    provides dev.webfx.stack.conf.spi.ConfigurationConsumer with dev.webfx.stack.com.bus.spi.impl.json.vertx.VertxBusConfigurationConsumer;

}