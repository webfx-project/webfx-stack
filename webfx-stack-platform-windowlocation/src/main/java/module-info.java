// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.platform.windowlocation {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.util;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.platform.windowlocation;
    exports dev.webfx.stack.platform.windowlocation.spi;
    exports dev.webfx.stack.platform.windowlocation.spi.impl;

    // Used services
    uses dev.webfx.stack.platform.windowlocation.spi.WindowLocationProvider;

}