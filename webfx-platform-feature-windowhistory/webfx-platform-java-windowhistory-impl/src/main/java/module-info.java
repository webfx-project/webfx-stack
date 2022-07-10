// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.java.windowhistory.impl {

    // Direct dependencies modules
    requires webfx.platform.client.windowhistory;

    // Exported packages
    exports dev.webfx.stack.platform.windowhistory.spi.impl.java;

    // Provided services
    provides dev.webfx.stack.platform.windowhistory.spi.WindowHistoryProvider with dev.webfx.stack.platform.windowhistory.spi.impl.java.JavaWindowHistoryProvider;

}