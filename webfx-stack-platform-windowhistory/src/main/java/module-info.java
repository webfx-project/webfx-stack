// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.platform.windowhistory {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;
    requires webfx.stack.platform.json;
    requires webfx.stack.platform.windowlocation;

    // Exported packages
    exports dev.webfx.stack.platform.windowhistory;
    exports dev.webfx.stack.platform.windowhistory.spi;
    exports dev.webfx.stack.platform.windowhistory.spi.impl;

    // Used services
    uses dev.webfx.stack.platform.windowhistory.spi.WindowHistoryProvider;

}