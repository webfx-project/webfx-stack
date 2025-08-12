// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.ui.gateway.webviewbased {

    // Direct dependencies modules
    requires javafx.graphics;
    requires javafx.web;
    requires webfx.platform.console;
    requires webfx.platform.service;
    requires webfx.stack.authn.login;
    requires webfx.stack.authn.login.ui.gateway;

    // Exported packages
    exports dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview;
    exports dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi;

    // Used services
    uses dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.LoginWebViewProvider;

}