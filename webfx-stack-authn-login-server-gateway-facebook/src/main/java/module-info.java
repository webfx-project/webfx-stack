// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.server.gateway.facebook {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.stack.authn.login.server.gateway;
    requires webfx.stack.conf;
    requires webfx.stack.conf.resource;
    requires webfx.stack.routing.router;
    requires webfx.stack.session.state;

    // Exported packages
    exports dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook;

    // Resources packages
    opens dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook;

    // Provided services
    provides dev.webfx.stack.authn.login.spi.impl.server.gateway.ServerLoginGatewayProvider with dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook.FacebookServerLoginGatewayProvider;
    provides dev.webfx.stack.conf.spi.ConfigurationConsumer with dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook.FacebookServerLoginGatewayConfigurationConsumer;

}