// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.buscall {

    // Direct dependencies modules
    requires webfx.stack.authn.login;
    requires webfx.stack.com.bus.call;

    // Exported packages
    exports dev.webfx.stack.authn.login.buscall;

    // Provided services
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with dev.webfx.stack.authn.login.buscall.GetLoginUiInputMethodEndpoint;

}