// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.server.gateway.mojoauth {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.util;
    requires webfx.stack.authn;
    requires webfx.stack.authn.login.server.gateway;
    requires webfx.stack.conf;
    requires webfx.stack.conf.resource;
    requires webfx.stack.push.server;
    requires webfx.stack.routing.router;
    requires webfx.stack.session;
    requires webfx.stack.session.state;

    // Exported packages
    exports dev.webfx.stack.authn.login.spi.impl.server.gateway.mojoauth;

    // Resources packages
    opens dev.webfx.stack.authn.login.spi.impl.server.gateway.mojoauth;

    // Provided services
    provides dev.webfx.stack.authn.login.spi.impl.server.gateway.ServerLoginGatewayProvider with dev.webfx.stack.authn.login.spi.impl.server.gateway.mojoauth.MojoAuthServerLoginGatewayProvider;
    provides dev.webfx.stack.conf.spi.ConfigurationConsumer with dev.webfx.stack.authn.login.spi.impl.server.gateway.mojoauth.MojoAuthServerLoginGatewayConfigurationConsumer;

}