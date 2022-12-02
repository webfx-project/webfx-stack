// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.server.gateway.google {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.stack.authn;
    requires webfx.stack.authn.logout.server;
    requires webfx.stack.authn.server.gateway;

    // Exported packages
    exports dev.webfx.stack.authn.spi.impl.server.gateway.google;

    // Provided services
    provides dev.webfx.stack.authn.spi.impl.server.gateway.ServerAuthenticationGatewayProvider with dev.webfx.stack.authn.spi.impl.server.gateway.google.GoogleServerAuthenticationGatewayProvider;

}