// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.shared.bus.impl.simple {

    // Direct dependencies modules
    requires webfx.platform.shared.bus;

    // Exported packages
    exports dev.webfx.stack.platform.shared.services.bus.spi.impl.simple;

    // Provided services
    provides dev.webfx.stack.platform.shared.services.bus.spi.BusServiceProvider with dev.webfx.stack.platform.shared.services.bus.spi.impl.simple.SimpleBusServiceProvider;

}