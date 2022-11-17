// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.authz {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.platform.async;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.stack.ui.operation;

    // Exported packages
    exports dev.webfx.stack.auth.authz;
    exports dev.webfx.stack.auth.authz.mixin;
    exports dev.webfx.stack.auth.authz.operation;
    exports dev.webfx.stack.auth.authz.spi;
    exports dev.webfx.stack.auth.authz.spi.impl;
    exports dev.webfx.stack.auth.authz.spi.impl.inmemory;
    exports dev.webfx.stack.auth.authz.spi.impl.inmemory.parser;

    // Used services
    uses dev.webfx.stack.auth.authz.spi.AuthorizationServiceProvider;

}