// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authz.client {

    // Direct dependencies modules
    requires javafx.base;
    requires webfx.extras.operation;
    requires webfx.platform.async;
    requires webfx.platform.service;
    requires webfx.platform.util;
    requires webfx.stack.session.state.client.fx;

    // Exported packages
    exports dev.webfx.stack.authz.client;
    exports dev.webfx.stack.authz.client.factory;
    exports dev.webfx.stack.authz.client.operation;
    exports dev.webfx.stack.authz.client.spi;
    exports dev.webfx.stack.authz.client.spi.impl;
    exports dev.webfx.stack.authz.client.spi.impl.inmemory;
    exports dev.webfx.stack.authz.client.spi.impl.inmemory.parser;

    // Used services
    uses dev.webfx.stack.authz.client.spi.AuthorizationClientServiceProvider;

}