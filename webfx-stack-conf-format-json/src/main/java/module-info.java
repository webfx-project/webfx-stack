// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.conf.format.json {

    // Direct dependencies modules
    requires webfx.platform.json;
    requires webfx.platform.util;
    requires webfx.stack.conf;

    // Exported packages
    exports dev.webfx.stack.conf.spi.impl.format.json;

    // Provided services
    provides dev.webfx.stack.conf.spi.ConfigurationFormat with dev.webfx.stack.conf.spi.impl.format.json.JsonConfigurationFormat;

}