// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.ui.gateway.password.plugin {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.panes;
    requires webfx.extras.util.animation;
    requires webfx.extras.util.layout;
    requires webfx.extras.util.scene;
    requires webfx.kit.util;
    requires webfx.platform.uischeduler;
    requires webfx.platform.windowlocation;
    requires webfx.stack.authn;
    requires webfx.stack.authn.login.ui;
    requires webfx.stack.authn.login.ui.gateway;
    requires webfx.stack.i18n;
    requires webfx.stack.i18n.controls;
    requires webfx.stack.ui.controls;

    // Exported packages
    exports dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;

    // Provided services
    provides dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProvider with dev.webfx.stack.authn.login.ui.spi.impl.gateway.password.PasswordUiLoginGatewayProvider;

}