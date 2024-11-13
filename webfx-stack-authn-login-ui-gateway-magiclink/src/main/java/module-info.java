// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.ui.gateway.magiclink {

    // Direct dependencies modules
    requires javafx.graphics;
    requires webfx.stack.authn.login.ui.gateway;
    requires webfx.stack.ui.controls;

    // Exported packages
    exports dev.webfx.stack.authn.login.ui.spi.impl.gateway.magiclink;

    // Provided services
    provides dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProvider with dev.webfx.stack.authn.login.ui.spi.impl.gateway.magiclink.MagicLinkUiLoginGatewayProvider;

}