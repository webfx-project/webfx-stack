// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.login.ui.webthirdpary {

    // Direct dependencies modules
    requires javafx.graphics;
    requires javafx.web;
    requires webfx.platform.console;
    requires webfx.stack.auth.login;
    requires webfx.stack.auth.login.ui;

    // Exported packages
    exports dev.webfx.stack.auth.login.ui.spi.impl.webthirdparty;

    // Provided services
    provides dev.webfx.stack.auth.login.ui.spi.LoginUiProvider with dev.webfx.stack.auth.login.ui.spi.impl.webthirdparty.WebThirdPartyLoginUiProvider;

}