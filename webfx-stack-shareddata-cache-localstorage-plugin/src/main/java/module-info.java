// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.shareddata.cache.localstorage.plugin {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.storage;
    requires webfx.stack.shareddata;
    requires webfx.stack.shareddata.ast;
    requires webfx.stack.shareddata.cache;
    requires webfx.stack.shareddata.cache.storage;

    // Exported packages
    exports dev.webfx.stack.shareddata.cache.localstorage;

    // Provided services
    provides dev.webfx.stack.shareddata.cache.spi.CachesProvider with dev.webfx.stack.shareddata.cache.localstorage.LocalStorageCachesProvider;

}