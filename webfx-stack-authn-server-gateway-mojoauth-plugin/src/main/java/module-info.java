// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.server.gateway.mojoauth.plugin {

    // Direct dependencies modules
    requires java.sdk;
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.stack.authn;
    requires webfx.stack.authn.login.server.gateway.mojoauth.plugin;
    requires webfx.stack.authn.server.gateway;

    // Exported packages
    exports dev.webfx.stack.authn.server.gateway.spi.mojoauth;

    // Provided services
    provides dev.webfx.stack.authn.server.gateway.spi.ServerAuthenticationGateway with dev.webfx.stack.authn.server.gateway.spi.mojoauth.MojoAuthServerAuthenticationGateway;

}