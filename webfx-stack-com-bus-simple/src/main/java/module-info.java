// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus.simple {

    // Direct dependencies modules
    requires webfx.stack.com.bus;

    // Exported packages
    exports dev.webfx.stack.com.bus.spi.impl.simple;

    // Provided services
    provides dev.webfx.stack.com.bus.spi.BusServiceProvider with dev.webfx.stack.com.bus.spi.impl.simple.SimpleBusServiceProvider;

}