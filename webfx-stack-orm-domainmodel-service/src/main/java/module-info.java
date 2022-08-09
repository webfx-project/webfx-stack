// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.domainmodel.service {

    // Direct dependencies modules
    requires java.base;
    requires webfx.extras.label;
    requires webfx.extras.type;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.stack.db.query;
    requires webfx.stack.orm.domainmodel;

    // Exported packages
    exports dev.webfx.stack.orm.domainmodel.service;
    exports dev.webfx.stack.orm.domainmodel.service.spi;

    // Used services
    uses dev.webfx.stack.orm.domainmodel.service.spi.DomainModelProvider;

}