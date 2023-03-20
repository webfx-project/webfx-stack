// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.ui.portal {

    // Direct dependencies modules
    requires java.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.util.layout;
    requires webfx.platform.util;
    requires webfx.stack.authn.login.ui;
    requires webfx.stack.authn.login.ui.gateway;

    // Exported packages
    exports dev.webfx.stack.authn.login.ui.spi.impl.portal;

    // Used services
    uses dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProvider;

    // Provided services
    provides dev.webfx.stack.authn.login.ui.spi.UiLoginServiceProvider with dev.webfx.stack.authn.login.ui.spi.impl.portal.UiLoginPortalProvider;

}