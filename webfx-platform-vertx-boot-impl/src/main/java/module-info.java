// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.vertx.boot.impl {

    // Direct dependencies modules
    requires java.base;
    requires vertx.bridge.common;
    requires vertx.core;
    requires vertx.web;
    requires webfx.platform.shared.boot;
    requires webfx.platform.shared.shutdown;
    requires webfx.platform.shared.util;
    requires webfx.platform.vertx.instance;

    // Exported packages
    exports dev.webfx.platform.vertx.services.boot.spi.impl;

    // Provided services
    provides dev.webfx.platform.shared.services.boot.spi.ApplicationBooterProvider with dev.webfx.platform.vertx.services.boot.spi.impl.VertxApplicationBooterVerticle;

}