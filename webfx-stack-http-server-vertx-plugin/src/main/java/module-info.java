// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.http.server.vertx.plugin {

    // Direct dependencies modules
    requires io.vertx.core;
    requires io.vertx.web;
    requires webfx.platform.ast;
    requires webfx.platform.boot;
    requires webfx.platform.conf;
    requires webfx.platform.console;
    requires webfx.platform.util.vertx;

    // Exported packages
    exports dev.webfx.stack.http.server.vertx;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.http.server.vertx.VertxHttpStarterJob;
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.http.server.vertx.VertxHttpModuleBooter;

}