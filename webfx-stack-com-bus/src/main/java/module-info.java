// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.scheduler;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.com.bus;
    exports dev.webfx.stack.com.bus.spi;
    exports dev.webfx.stack.com.bus.spi.impl;

    // Used services
    uses dev.webfx.stack.com.bus.spi.BusServiceProvider;

}