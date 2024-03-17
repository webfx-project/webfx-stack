// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.session.vertx {

    // Direct dependencies modules
    requires io.vertx.core;
    requires io.vertx.web;
    requires webfx.platform.async;
    requires webfx.platform.vertx.common;
    requires webfx.stack.session;

    // Exported packages
    exports dev.webfx.stack.session.spi.impl.vertx;

    // Provided services
    provides dev.webfx.stack.session.spi.SessionServiceProvider with dev.webfx.stack.session.spi.impl.vertx.VertxSessionServiceProvider;

}