// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.server.gateway.facebook {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.stack.authn;
    requires webfx.stack.authn.logout.server;
    requires webfx.stack.authn.server.gateway;

    // Exported packages
    exports dev.webfx.stack.authn.spi.impl.server.gateway.facebook;

    // Provided services
    provides dev.webfx.stack.authn.spi.impl.server.gateway.ServerAuthenticationGatewayProvider with dev.webfx.stack.authn.spi.impl.server.gateway.facebook.FacebookServerAuthenticationGatewayProvider;

}