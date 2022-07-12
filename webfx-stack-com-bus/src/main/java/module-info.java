// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.console;
    requires webfx.platform.scheduler;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.com.bus;
    exports dev.webfx.stack.com.bus.spi;
    exports dev.webfx.stack.com.bus.spi.impl;

    // Used services
    uses dev.webfx.stack.com.bus.spi.BusServiceProvider;

}