// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.routing.router.vertx {

    // Direct dependencies modules
    requires io.vertx.core;
    requires io.vertx.web;
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.vertx.common;
    requires webfx.stack.routing.router;
    requires webfx.stack.session;
    requires webfx.stack.session.vertx;

    // Exported packages
    exports dev.webfx.stack.routing.router.spi.impl.vertx;

    // Provided services
    provides dev.webfx.stack.routing.router.spi.RouterFactoryProvider with dev.webfx.stack.routing.router.spi.impl.vertx.VertxRouterFactoryProvider;

}