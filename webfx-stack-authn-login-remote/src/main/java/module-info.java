// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.remote {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.stack.authn.login;
    requires webfx.stack.authn.login.buscall;
    requires webfx.stack.com.bus.call;

    // Exported packages
    exports dev.webfx.stack.authn.login.spi.impl.remote;

    // Provided services
    provides dev.webfx.stack.authn.login.spi.LoginServiceProvider with dev.webfx.stack.authn.login.spi.impl.remote.RemoteLoginServiceProvider;

}