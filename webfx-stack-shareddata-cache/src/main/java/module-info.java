// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.shareddata.cache {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.scheduler;
    requires webfx.platform.service;
    requires webfx.platform.util;
    requires webfx.stack.shareddata;

    // Exported packages
    exports dev.webfx.stack.shareddata.cache;
    exports dev.webfx.stack.shareddata.cache.spi;

    // Used services
    uses dev.webfx.stack.shareddata.cache.spi.CachesProvider;

}