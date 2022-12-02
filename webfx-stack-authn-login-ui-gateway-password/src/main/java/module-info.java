// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.ui.gateway.password {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.kit.util;
    requires webfx.platform.console;
    requires webfx.stack.authn;
    requires webfx.stack.authn.login.ui.gateway;
    requires webfx.stack.i18n;
    requires webfx.stack.ui.controls;
    requires webfx.stack.ui.util;

    // Exported packages
    exports dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;

    // Provided services
    provides dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProvider with dev.webfx.stack.authn.login.ui.spi.impl.gateway.password.PasswordUiLoginGatewayProvider;

}