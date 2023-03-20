// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.ui.fxraiser.json {

    // Direct dependencies modules
    requires javafx.graphics;
    requires webfx.platform.boot;
    requires webfx.platform.json;
    requires webfx.platform.util;
    requires webfx.stack.ui.fxraiser;
    requires webfx.stack.ui.json;

    // Exported packages
    exports dev.webfx.stack.ui.fxraiser.json;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.ui.fxraiser.json.JsonFXRaiserModuleBooter;

}