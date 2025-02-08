// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.ui.gateway.password.plugin {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.panes;
    requires webfx.extras.styles.bootstrap;
    requires webfx.extras.util.control;
    requires webfx.extras.util.scene;
    requires webfx.kit.util;
    requires webfx.platform.uischeduler;
    requires webfx.platform.windowlocation;
    requires webfx.stack.authn;
    requires webfx.stack.authn.login.ui;
    requires webfx.stack.authn.login.ui.gateway;
    requires webfx.stack.i18n;
    requires webfx.stack.i18n.controls;
    requires webfx.stack.session.state.client.fx;
    requires webfx.stack.ui.controls;
    requires webfx.stack.ui.operation;
    requires webfx.stack.ui.validation;

    // Exported packages
    exports dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;

    // Provided services
    provides dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGateway with dev.webfx.stack.authn.login.ui.spi.impl.gateway.password.PasswordUiLoginGateway;

}