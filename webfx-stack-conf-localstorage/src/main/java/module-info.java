// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.conf.localstorage {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.storage;
    requires webfx.stack.conf;

    // Exported packages
    exports dev.webfx.stack.conf.spi.impl.localstorage;

    // Provided services
    provides dev.webfx.stack.conf.spi.ConfigurationSupplier with dev.webfx.stack.conf.spi.impl.localstorage.LocalStorageConfigurationSupplier;

}