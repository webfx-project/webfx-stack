// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.cloud.deepl.server.plugin {

    // Direct dependencies modules
    requires io.vertx.core;
    requires io.vertx.web;
    requires webfx.platform.boot;
    requires webfx.platform.conf;
    requires webfx.platform.fetch;
    requires webfx.platform.util.vertx;

    // Exported packages
    exports dev.webfx.stack.cloud.deepl.server;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.cloud.deepl.server.ServerDeeplModuleBooter;

}