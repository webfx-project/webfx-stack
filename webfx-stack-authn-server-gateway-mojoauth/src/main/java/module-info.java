// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.server.gateway.mojoauth {

    // Direct dependencies modules
    requires java.base;
    requires java.sdk;
    requires webfx.platform.async;
    requires webfx.platform.json;
    requires webfx.stack.authn;
    requires webfx.stack.authn.logout.server;
    requires webfx.stack.authn.server.gateway;
    requires webfx.stack.session.state;

    // Exported packages
    exports dev.webfx.stack.authn.spi.impl.server.gateway.mojoauth;

    // Provided services
    provides dev.webfx.stack.authn.spi.impl.server.gateway.ServerAuthenticationGatewayProvider with dev.webfx.stack.authn.spi.impl.server.gateway.mojoauth.MojoAuthServerAuthenticationGatewayProvider;

}