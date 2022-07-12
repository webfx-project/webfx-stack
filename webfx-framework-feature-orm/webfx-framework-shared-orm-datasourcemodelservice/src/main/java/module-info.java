// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.orm.datasourcemodelservice {

    // Direct dependencies modules
    requires java.base;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.platform.util;
    requires webfx.stack.async;

    // Exported packages
    exports dev.webfx.stack.framework.shared.services.datasourcemodel;
    exports dev.webfx.stack.framework.shared.services.datasourcemodel.spi;

    // Used services
    uses dev.webfx.stack.framework.shared.services.datasourcemodel.spi.DataSourceModelProvider;

}