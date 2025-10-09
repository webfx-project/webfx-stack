// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.server.portal {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.service;
    requires webfx.stack.authn;
    requires webfx.stack.authn.server.gateway;
    requires webfx.stack.com.bus;
    requires webfx.stack.session.state;
    requires webfx.stack.session.state.server;

    // Exported packages
    exports dev.webfx.stack.authn.spi.impl.server;
    exports dev.webfx.stack.authn.spi.impl.server.portal;

    // Used services
    uses dev.webfx.stack.authn.server.gateway.spi.ServerAuthenticationGateway;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.authn.spi.impl.server.ServerAuthenticationJob;
    provides dev.webfx.stack.authn.spi.AuthenticationServiceProvider with dev.webfx.stack.authn.spi.impl.server.portal.ServerAuthenticationPortalProvider;

}