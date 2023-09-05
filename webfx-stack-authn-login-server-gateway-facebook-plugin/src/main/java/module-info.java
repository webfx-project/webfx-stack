// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.server.gateway.facebook.plugin {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.conf;
    requires webfx.platform.console;
    requires webfx.stack.authn;
    requires webfx.stack.authn.login.server.gateway;
    requires webfx.stack.push.server;
    requires webfx.stack.routing.router;
    requires webfx.stack.session;
    requires webfx.stack.session.state;

    // Exported packages
    exports dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook;

    // Provided services
    provides dev.webfx.stack.authn.login.spi.impl.server.gateway.ServerLoginGatewayProvider with dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook.FacebookServerLoginGatewayProvider;

}