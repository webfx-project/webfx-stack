package dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.impl.gwt;

import dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.LoginWebViewProvider;
import javafx.scene.web.WebView;

/**
 * @author Bruno Salmon
 */
public class GwtLoginWebViewProvider implements LoginWebViewProvider {

    @Override
    public WebView createLoginWebView() {
        return new WebView();
    }

    @Override
    public boolean isWebViewInIFrame() {
        return true;
    }
}
