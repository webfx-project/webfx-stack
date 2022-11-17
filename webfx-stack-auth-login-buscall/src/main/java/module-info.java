// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.login.buscall {

    // Direct dependencies modules
    requires webfx.stack.auth.login;
    requires webfx.stack.com.bus.call;

    // Exported packages
    exports dev.webfx.stack.auth.login.buscall;

    // Provided services
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with dev.webfx.stack.auth.login.buscall.GetLoginUiInputMethodEndpoint;

}