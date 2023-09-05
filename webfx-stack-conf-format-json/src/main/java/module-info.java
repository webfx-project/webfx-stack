// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.conf.format.json {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires webfx.stack.conf;

    // Exported packages
    exports dev.webfx.stack.conf.spi.impl.format.json;

    // Provided services
    provides dev.webfx.stack.conf.spi.ConfigurationFormat with dev.webfx.stack.conf.spi.impl.format.json.JsonConfigurationFormat;

}