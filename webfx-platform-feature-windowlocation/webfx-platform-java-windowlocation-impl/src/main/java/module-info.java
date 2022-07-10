// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.java.windowlocation.impl {

    // Direct dependencies modules
    requires webfx.platform.client.websocketbus;
    requires webfx.platform.client.windowhistory;
    requires webfx.platform.client.windowlocation;
    requires webfx.platform.shared.bus;

    // Exported packages
    exports dev.webfx.stack.platform.windowlocation.spi.impl.java;

    // Provided services
    provides dev.webfx.stack.platform.windowlocation.spi.WindowLocationProvider with dev.webfx.stack.platform.windowlocation.spi.impl.java.JavaWindowLocationProvider;

}