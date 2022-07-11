// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.platform.json.java {

    // Direct dependencies modules
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.platform.json.spi.impl.java;

    // Provided services
    provides dev.webfx.stack.platform.json.spi.JsonProvider with dev.webfx.stack.platform.json.spi.impl.java.JavaJsonProvider;

}