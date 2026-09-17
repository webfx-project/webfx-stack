// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authz.client {

    // Direct dependencies modules
    requires javafx.base;
    requires webfx.platform.async;
    requires webfx.platform.service;
    requires webfx.platform.util;
    requires webfx.stack.authz.core;
    requires webfx.stack.session.state;
    requires webfx.stack.session.state.client.fx;

    // Exported packages
    exports dev.webfx.stack.authz.client;
    exports dev.webfx.stack.authz.client.binder;
    exports dev.webfx.stack.authz.client.context;
    exports dev.webfx.stack.authz.client.factory;
    exports dev.webfx.stack.authz.client.spi;
    exports dev.webfx.stack.authz.client.spi.impl;

    // Used services
    uses dev.webfx.stack.authz.client.spi.AuthorizationClientServiceProvider;

}