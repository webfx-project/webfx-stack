// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.ui.gateway.password.plugin {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.async;
    requires webfx.extras.controlfactory;
    requires webfx.extras.i18n;
    requires webfx.extras.i18n.controls;
    requires webfx.extras.panes;
    requires webfx.extras.styles.bootstrap;
    requires webfx.extras.util.control;
    requires webfx.extras.util.layout;
    requires webfx.extras.util.scene;
    requires webfx.extras.validation;
    requires webfx.kit.util;
    requires webfx.platform.uischeduler;
    requires webfx.platform.windowlocation;
    requires webfx.stack.authn;
    requires webfx.stack.authn.login.ui;
    requires webfx.stack.authn.login.ui.gateway;
    requires webfx.stack.session.state.client.fx;

    // Exported packages
    exports dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;

    // Resources packages
    opens dev.webfx.kit.css.fonts.password;

    // Provided services
    provides dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGateway with dev.webfx.stack.authn.login.ui.spi.impl.gateway.password.PasswordUiLoginGateway;

}