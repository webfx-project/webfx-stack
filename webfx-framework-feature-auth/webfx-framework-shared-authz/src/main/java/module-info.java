// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.authz {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.framework.shared.operation;
    requires webfx.platform.client.uischeduler;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.framework.shared.operation.authz;
    exports dev.webfx.framework.shared.services.authz;
    exports dev.webfx.framework.shared.services.authz.mixin;
    exports dev.webfx.framework.shared.services.authz.spi;
    exports dev.webfx.framework.shared.services.authz.spi.impl;
    exports dev.webfx.framework.shared.services.authz.spi.impl.inmemory;
    exports dev.webfx.framework.shared.services.authz.spi.impl.inmemory.parser;

    // Used services
    uses dev.webfx.framework.shared.services.authz.spi.AuthorizationServiceProvider;

}