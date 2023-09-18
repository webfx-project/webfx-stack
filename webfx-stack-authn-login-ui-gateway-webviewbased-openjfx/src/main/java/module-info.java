// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.ui.gateway.webviewbased.openjfx {

    // Direct dependencies modules
    requires java.base;
    requires java.xml;
    requires javafx.graphics;
    requires javafx.web;
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.storagelocation;
    requires webfx.stack.authn.login.ui.gateway.webviewbased;
    requires webfx.stack.com.serial;
    requires webfx.stack.ui.dialog;

    // Exported packages
    exports dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.impl.openjfx;

    // Provided services
    provides dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.LoginWebViewProvider with dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.impl.openjfx.FXLoginWebViewProvider;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.impl.openjfx.HttpCookieSerialCodec;

}