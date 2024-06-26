// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.datasourcemodel.service {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.service;
    requires webfx.stack.orm.domainmodel;

    // Exported packages
    exports dev.webfx.stack.orm.datasourcemodel.service;
    exports dev.webfx.stack.orm.datasourcemodel.service.spi;

    // Used services
    uses dev.webfx.stack.orm.datasourcemodel.service.spi.DataSourceModelProvider;

}