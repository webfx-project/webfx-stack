// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.ui.gateway.google {

    // Direct dependencies modules
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.platform.resource;
    requires webfx.stack.authn.login.ui.gateway;
    requires webfx.stack.authn.login.ui.gateway.webviewbased;

    // Exported packages
    exports dev.webfx.stack.authn.login.ui.spi.impl.gateway.google;

    // Resources packages
    opens dev.webfx.stack.authn.login.ui.spi.impl.gateway;
    opens dev.webfx.stack.authn.login.ui.spi.impl.gateway.google;

    // Provided services
    provides dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProvider with dev.webfx.stack.authn.login.ui.spi.impl.gateway.google.GoogleUiLoginGatewayProvider;

}