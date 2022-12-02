package dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview;

import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authn.login.LoginService;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProviderBase;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * @author Bruno Salmon
 */
public abstract class WebViewBasedUiLoginGatewayProvider extends UiLoginGatewayProviderBase {

    private final static String ERROR_HTML_TEMPLATE = "<html><body><center>{{ERROR}}</center></body></html>";

    public WebViewBasedUiLoginGatewayProvider(Object gatewayId) {
        super(gatewayId);
    }

    @Override
    public Node createLoginUi() {
        WebView webView = new WebView();
        LoginService.getLoginUiInput(getGatewayId())
                .onComplete(ar -> UiScheduler.runInUiThread(() -> {
                    String input = null;
                    if (ar.failed())
                        input = ERROR_HTML_TEMPLATE.replace("{{ERROR}}", ar.cause().getMessage());
                    else if (ar.result() instanceof String)
                        input = (String) ar.result();
                    if (input != null) {
                        WebEngine webEngine = webView.getEngine();
                        if (input.startsWith("http"))
                            webEngine.load(input);
                        else
                            webEngine.loadContent(input);
                    }
                }));
        return webView;
    }

}
