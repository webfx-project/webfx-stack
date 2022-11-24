// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.authz.client {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.platform.async;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.stack.session.state.client.fx;
    requires webfx.stack.ui.operation;

    // Exported packages
    exports dev.webfx.stack.auth.authz.client;
    exports dev.webfx.stack.auth.authz.client.factory;
    exports dev.webfx.stack.auth.authz.client.operation;
    exports dev.webfx.stack.auth.authz.client.spi;
    exports dev.webfx.stack.auth.authz.client.spi.impl;
    exports dev.webfx.stack.auth.authz.client.spi.impl.inmemory;
    exports dev.webfx.stack.auth.authz.client.spi.impl.inmemory.parser;

    // Used services
    uses dev.webfx.stack.auth.authz.client.spi.AuthorizationClientServiceProvider;

}