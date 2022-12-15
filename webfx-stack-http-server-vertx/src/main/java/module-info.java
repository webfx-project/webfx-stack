// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.http.server.vertx {

    // Direct dependencies modules
    requires io.vertx.core;
    requires io.vertx.web;
    requires java.base;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.platform.vertx.common;
    requires webfx.stack.conf;
    requires webfx.stack.conf.resource;

    // Exported packages
    exports dev.webfx.stack.http.server.vertx;

    // Resources packages
    opens dev.webfx.stack.http.server.vertx;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.http.server.vertx.VertxHttpStarterJob;
    provides dev.webfx.stack.conf.spi.ConfigurationConsumer with dev.webfx.stack.http.server.vertx.VertxHttpConfigurationConsumer;
    provides dev.webfx.stack.conf.spi.ConfigurationSupplier with dev.webfx.stack.http.server.vertx.VertxHttpConfigurationSupplier;

}