// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.orm.domainmodelservice {

    // Direct dependencies modules
    requires java.base;
    requires webfx.extras.label;
    requires webfx.extras.type;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.query;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.stack.framework.shared.services.domainmodel;
    exports dev.webfx.stack.framework.shared.services.domainmodel.spi;

    // Used services
    uses dev.webfx.stack.framework.shared.services.domainmodel.spi.DomainModelProvider;

}