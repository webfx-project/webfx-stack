// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires transitive webfx.platform.async;
    requires webfx.platform.service;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.com.bus;
    exports dev.webfx.stack.com.bus.spi;

    // Used services
    uses dev.webfx.stack.com.bus.spi.BusServiceProvider;

}