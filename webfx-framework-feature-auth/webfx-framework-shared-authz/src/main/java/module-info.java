// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.authz {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.framework.shared.operation;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.stack.async;

    // Exported packages
    exports dev.webfx.stack.framework.shared.operation.authz;
    exports dev.webfx.stack.framework.shared.services.authz;
    exports dev.webfx.stack.framework.shared.services.authz.mixin;
    exports dev.webfx.stack.framework.shared.services.authz.spi;
    exports dev.webfx.stack.framework.shared.services.authz.spi.impl;
    exports dev.webfx.stack.framework.shared.services.authz.spi.impl.inmemory;
    exports dev.webfx.stack.framework.shared.services.authz.spi.impl.inmemory.parser;

    // Used services
    uses dev.webfx.stack.framework.shared.services.authz.spi.AuthorizationServiceProvider;

}