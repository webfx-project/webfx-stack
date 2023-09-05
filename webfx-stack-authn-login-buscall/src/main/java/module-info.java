// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.buscall {

    // Direct dependencies modules
    requires webfx.platform.ast.json.plugin;
    requires webfx.stack.authn.login;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.com.serial;

    // Exported packages
    exports dev.webfx.stack.authn.login.buscall;
    exports dev.webfx.stack.authn.login.buscall.serial;

    // Provided services
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with dev.webfx.stack.authn.login.buscall.GetLoginUiInputMethodEndpoint;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.authn.login.buscall.serial.LoginUiContextSerialCodec;

}