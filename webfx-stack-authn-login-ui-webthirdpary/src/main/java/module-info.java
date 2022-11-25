// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.ui.webthirdpary {

    // Direct dependencies modules
    requires javafx.graphics;
    requires javafx.web;
    requires webfx.platform.console;
    requires webfx.stack.authn.login;
    requires webfx.stack.authn.login.ui;

    // Exported packages
    exports dev.webfx.stack.authn.login.ui.spi.impl.webthirdparty;

    // Provided services
    provides dev.webfx.stack.authn.login.ui.spi.LoginUiProvider with dev.webfx.stack.authn.login.ui.spi.impl.webthirdparty.WebThirdPartyLoginUiProvider;

}