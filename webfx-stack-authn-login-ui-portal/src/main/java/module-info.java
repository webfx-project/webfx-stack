// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.ui.portal {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.panes;
    requires webfx.extras.util.animation;
    requires webfx.kit.util;
    requires webfx.platform.console;
    requires webfx.platform.os;
    requires webfx.platform.service;
    requires webfx.platform.uischeduler;
    requires webfx.stack.authn.login.ui;
    requires webfx.stack.authn.login.ui.gateway;
    requires webfx.stack.authn.login.ui.gateway.magiclink.plugin;

    // Exported packages
    exports dev.webfx.stack.authn.login.ui.spi.impl.portal;

    // Resources packages
    opens dev.webfx.kit.css.fonts.password;
    opens dev.webfx.kit.css.loginportal.images;

    // Used services
    uses dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProvider;

    // Provided services
    provides dev.webfx.stack.authn.login.ui.spi.UiLoginServiceProvider with dev.webfx.stack.authn.login.ui.spi.impl.portal.UiLoginPortalProvider;

}