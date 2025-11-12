// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.cloud.image.rest.server.plugin {

    // Direct dependencies modules
    requires io.vertx.core;
    requires io.vertx.web;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.conf;
    requires webfx.platform.file.java;
    requires webfx.platform.util;
    requires webfx.platform.util.http;
    requires webfx.platform.util.vertx;
    requires webfx.stack.cloud.image;

    // Exported packages
    exports dev.webfx.stack.cloud.image.impl.rest.server;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.cloud.image.impl.rest.server.RestServerCloudImageServiceBooter;

}