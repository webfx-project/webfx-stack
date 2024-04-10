package dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview;

import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.LoginWebViewProvider;
import javafx.scene.web.WebView;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class LoginWebViewService {

    public static LoginWebViewProvider getProvider() {
        return SingleServiceProvider.getProvider(LoginWebViewProvider.class, () -> ServiceLoader.load(LoginWebViewProvider.class));
    }

    public static WebView createLoginWebView() {
        return getProvider().createLoginWebView();
    }

    public static boolean isWebViewInIFrame() {
        return getProvider().isWebViewInIFrame();
    }

}
