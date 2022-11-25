// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.remote {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.stack.authn;
    requires webfx.stack.authn.buscall;
    requires webfx.stack.com.bus.call;

    // Exported packages
    exports dev.webfx.stack.authn.spi.impl.remote;

    // Provided services
    provides dev.webfx.stack.authn.spi.AuthenticationServiceProvider with dev.webfx.stack.authn.spi.impl.remote.RemoteAuthenticationServiceProvider;

}