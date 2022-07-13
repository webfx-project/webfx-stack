// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.platform.windowlocation.java {

    // Direct dependencies modules
    requires webfx.stack.com.bus;
    requires webfx.stack.com.websocket.bus;
    requires webfx.stack.platform.windowhistory;
    requires webfx.stack.platform.windowlocation;

    // Exported packages
    exports dev.webfx.stack.platform.windowlocation.spi.impl.java;

    // Provided services
    provides dev.webfx.stack.platform.windowlocation.spi.WindowLocationProvider with dev.webfx.stack.platform.windowlocation.spi.impl.java.JavaWindowLocationProvider;

}