// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.conf {

    // Direct dependencies modules
    requires java.base;
    requires transitive webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.json;
    requires transitive webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.conf;
    exports dev.webfx.stack.conf.spi;
    exports dev.webfx.stack.conf.spi.impl;

    // Used services
    uses dev.webfx.stack.conf.spi.ConfigurationConsumer;
    uses dev.webfx.stack.conf.spi.ConfigurationFormat;
    uses dev.webfx.stack.conf.spi.ConfigurationServiceProvider;
    uses dev.webfx.stack.conf.spi.ConfigurationSupplier;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.conf.spi.impl.ConfigurationModuleBooter;

}