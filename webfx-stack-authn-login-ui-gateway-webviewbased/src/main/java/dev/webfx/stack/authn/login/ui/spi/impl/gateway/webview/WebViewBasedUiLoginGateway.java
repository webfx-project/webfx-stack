package dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview;

import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authn.login.LoginService;
import dev.webfx.stack.authn.login.LoginUiContext;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayBase;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * @author Bruno Salmon
 */
public abstract class WebViewBasedUiLoginGateway extends UiLoginGatewayBase {

    private final static String ERROR_HTML_TEMPLATE = "<html><body><center>{{ERROR}}</center></body></html>";

    public WebViewBasedUiLoginGateway(Object gatewayId) {
        super(gatewayId);
    }

    @Override
    public Node createLoginUi(UiLoginPortalCallback callback) {
        WebView loginWebView = LoginWebViewService.createLoginWebView();
        WebEngine loginWebEngine = loginWebView.getEngine();
        loginWebEngine.setOnError(e -> callback.notifyInitializationFailure());
        // Now that our web view is correctly set up to start a login process, we call the login service to get the UI
        // input, which - in a case of a web view - should be the either a URL to load, or directly a HTML content.
        LoginService.getLoginUiInput(new LoginUiContext(getGatewayId(), LoginWebViewService.isWebViewInIFrame()))
                .onComplete(ar -> UiScheduler.runInUiThread(() -> {
                    String input = null;
                    if (ar.failed()) {
                        input = ERROR_HTML_TEMPLATE.replace("{{ERROR}}", ar.cause().getMessage());
                        callback.notifyInitializationFailure();
                    } else if (ar.result() instanceof String)
                        input = (String) ar.result();
                    if (input != null) {
                        Console.log("WebView input = " + input);
                        if (input.startsWith("http"))
                            loginWebEngine.load(input);
                        else
                            loginWebEngine.loadContent(input);
                    }
                }));
        return loginWebView;
    }

}
