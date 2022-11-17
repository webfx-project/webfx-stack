// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.authn.remote {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.stack.auth.authn;
    requires webfx.stack.auth.authn.buscall;
    requires webfx.stack.com.bus.call;

    // Exported packages
    exports dev.webfx.stack.auth.authn.spi.impl.remote;

    // Provided services
    provides dev.webfx.stack.auth.authn.spi.AuthenticationServiceProvider with dev.webfx.stack.auth.authn.spi.impl.remote.RemoteAuthenticationServiceProvider;

}