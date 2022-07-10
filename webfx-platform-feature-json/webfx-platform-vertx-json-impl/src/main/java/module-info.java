// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.vertx.json.impl {

    // Direct dependencies modules
    requires io.vertx.core;
    requires java.base;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.stack.platform.vertx.services.json.spi.impl;

    // Provided services
    provides dev.webfx.stack.platform.json.spi.JsonProvider with dev.webfx.stack.platform.vertx.services.json.spi.impl.VertxJsonObject;

}