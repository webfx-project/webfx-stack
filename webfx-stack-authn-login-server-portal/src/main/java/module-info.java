// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.server.portal {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.service;
    requires webfx.stack.authn.login;
    requires webfx.stack.authn.login.server.gateway;

    // Exported packages
    exports dev.webfx.stack.authn.login.spi.impl.server.portal;

    // Used services
    uses dev.webfx.stack.authn.login.spi.impl.server.gateway.ServerLoginGateway;

    // Provided services
    provides dev.webfx.stack.authn.login.spi.LoginServiceProvider with dev.webfx.stack.authn.login.spi.impl.server.portal.ServerLoginPortalProvider;

}