// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.authn.server.mojoauth {

    // Direct dependencies modules
    requires java.sdk;
    requires webfx.platform.async;
    requires webfx.stack.auth.authn;

    // Exported packages
    exports dev.webfx.stack.auth.authn.spi.impl.mojoauth;

    // Provided services
    provides dev.webfx.stack.auth.authn.spi.AuthenticationServiceProvider with dev.webfx.stack.auth.authn.spi.impl.mojoauth.MojoAuthAuthenticationServiceProvider;

}