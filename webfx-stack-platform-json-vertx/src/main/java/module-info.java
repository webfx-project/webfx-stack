// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.platform.json.vertx {

    // Direct dependencies modules
    requires io.vertx.core;
    requires java.base;
    requires webfx.platform.util;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.platform.json.spi.impl.vertx;

    // Provided services
    provides dev.webfx.stack.platform.json.spi.JsonProvider with dev.webfx.stack.platform.json.spi.impl.vertx.VertxJsonObject;

}