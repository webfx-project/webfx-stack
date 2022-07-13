// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authz {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.ui.operation;

    // Exported packages
    exports dev.webfx.stack.authz;
    exports dev.webfx.stack.authz.mixin;
    exports dev.webfx.stack.authz.operation;
    exports dev.webfx.stack.authz.spi;
    exports dev.webfx.stack.authz.spi.impl;
    exports dev.webfx.stack.authz.spi.impl.inmemory;
    exports dev.webfx.stack.authz.spi.impl.inmemory.parser;

    // Used services
    uses dev.webfx.stack.authz.spi.AuthorizationServiceProvider;

}