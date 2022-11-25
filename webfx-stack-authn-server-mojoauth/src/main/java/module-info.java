// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.server.mojoauth {

    // Direct dependencies modules
    requires java.base;
    requires java.sdk;
    requires webfx.platform.async;
    requires webfx.platform.json;
    requires webfx.stack.authn;

    // Exported packages
    exports dev.webfx.stack.authn.spi.impl.mojoauth;

    // Provided services
    provides dev.webfx.stack.authn.spi.AuthenticationServiceProvider with dev.webfx.stack.authn.spi.impl.mojoauth.MojoAuthAuthenticationServiceProvider;

}