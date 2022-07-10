// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.shared.json.impl {

    // Direct dependencies modules
    requires webfx.platform.shared.json;

    // Exported packages
    exports dev.webfx.stack.platform.json.spi.impl.java;

    // Provided services
    provides dev.webfx.stack.platform.json.spi.JsonProvider with dev.webfx.stack.platform.json.spi.impl.java.JavaJsonProvider;

}