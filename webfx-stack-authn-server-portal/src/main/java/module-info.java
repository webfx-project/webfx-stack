// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.server.portal {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.stack.authn;
    requires webfx.stack.authn.server.gateway;
    requires webfx.stack.session.state.server;

    // Exported packages
    exports dev.webfx.stack.authn.spi.impl.server;
    exports dev.webfx.stack.authn.spi.impl.server.portal;

    // Used services
    uses dev.webfx.stack.authn.spi.impl.server.gateway.ServerAuthenticationGatewayProvider;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.authn.spi.impl.server.ServerAuthenticationJob;
    provides dev.webfx.stack.authn.spi.AuthenticationServiceProvider with dev.webfx.stack.authn.spi.impl.server.portal.ServerAuthenticationPortalProvider;

}