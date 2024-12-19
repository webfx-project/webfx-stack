// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.server.gateway.facebook.plugin {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.fetch.ast.json;
    requires webfx.stack.authn;
    requires webfx.stack.authn.login.server.gateway.facebook.plugin;
    requires webfx.stack.authn.server.gateway;
    requires webfx.stack.session.state;

    // Exported packages
    exports dev.webfx.stack.authn.server.gateway.spi.impl.facebook;

    // Provided services
    provides dev.webfx.stack.authn.server.gateway.spi.ServerAuthenticationGateway with dev.webfx.stack.authn.server.gateway.spi.impl.facebook.FacebookServerAuthenticationGateway;

}