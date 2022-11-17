// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.login.remote {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.stack.auth.login;
    requires webfx.stack.auth.login.buscall;
    requires webfx.stack.com.bus.call;

    // Exported packages
    exports dev.webfx.stack.auth.login.spi.impl.remote;

    // Provided services
    provides dev.webfx.stack.auth.login.spi.LoginServiceProvider with dev.webfx.stack.auth.login.spi.impl.remote.RemoteLoginServiceProvider;

}