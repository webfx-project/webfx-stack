package dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi;

import javafx.scene.web.WebView;

public interface LoginWebViewProvider {

    WebView createLoginWebView();

    boolean isWebViewInIFrame();

}
